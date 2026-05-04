package com.expensetracker.service;

import com.expensetracker.dto.BankStatementImportResult;
import com.expensetracker.dto.BankStatementTransactionDTO;
import com.expensetracker.exception.BankStatementProcessingException;
import com.expensetracker.exception.UserNotFoundException;
import com.expensetracker.model.Expense;
import com.expensetracker.model.Income;
import com.expensetracker.model.User;
import com.expensetracker.model.UserExpenseCategory;
import com.expensetracker.repository.ExpenseRepository;
import com.expensetracker.repository.IncomeRepository;
import com.expensetracker.repository.UserRepository;
import com.expensetracker.repository.UserExpenseCategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Orchestrates importing transactions from an HDFC bank statement PDF into
 * the expense / income tables.
 *
 * <h3>Import algorithm</h3>
 * <ol>
 *   <li>Parse all transactions from the PDF via {@link HdfcStatementParserService}.</li>
 *   <li>Fetch the user's {@code current_closing_balance} from the {@code users} table.</li>
 *   <li>Find the first transaction in the statement whose closing balance equals
 *       the user's current closing balance.  If none is found, throw a
 *       {@link BankStatementProcessingException} asking the user to upload a more
 *       recent statement.</li>
 *   <li>Process every transaction <em>after</em> the match point:
 *       <ul>
 *         <li>Closing balance decreased  → withdrawal  → add {@link Expense}.</li>
 *         <li>Closing balance increased  → deposit     → add {@link Income}.</li>
 *         <li>No change                  → skip.</li>
 *       </ul>
 *   </li>
 * </ol>
 */
@Service
public class BankStatementImportService {

    private static final Logger logger = LoggerFactory.getLogger(BankStatementImportService.class);

    /** Maximum length for expense_name / income.source columns (DB constraint). */
    private static final int MAX_NAME_LENGTH = 100;

    private final HdfcStatementParserService parserService;
    private final UserRepository userRepository;
    private final UserExpenseCategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final ClosingBalanceService closingBalanceService;

    @Autowired
    public BankStatementImportService(HdfcStatementParserService parserService,
                                      UserRepository userRepository,
                                      UserExpenseCategoryRepository categoryRepository,
                                      ExpenseRepository expenseRepository,
                                      IncomeRepository incomeRepository,
                                      ClosingBalanceService closingBalanceService) {
        this.parserService        = parserService;
        this.userRepository       = userRepository;
        this.categoryRepository   = categoryRepository;
        this.expenseRepository    = expenseRepository;
        this.incomeRepository     = incomeRepository;
        this.closingBalanceService = closingBalanceService;
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /** Preferred category name for bank-statement imports. */
    private static final String PREFERRED_CATEGORY = "House Expenses";
    /** Fallback category name. */
    private static final String FALLBACK_CATEGORY  = "Miscellaneous";

    /**
     * Import transactions from an HDFC bank statement PDF.
     *
     * <p>All withdrawal transactions (expenses) are placed under the user's
     * <em>House Expenses</em> category.  If that category does not exist, the
     * first available active category is used instead, and a message is added
     * to the result to inform the user.
     *
     * @param file     the uploaded PDF file
     * @param userId   the user performing the import
     * @param password PDF password (may be {@code null} or blank)
     * @return summary of what was imported
     */
    @Transactional
    public BankStatementImportResult importStatement(MultipartFile file,
                                                     String userId,
                                                     String password) {
        // ── 1. Validate user ────────────────────────────────────────────────
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // ── 2. Resolve expense category (auto-select) ────────────────────────
        List<String> messages = new ArrayList<>();
        Integer resolvedCategoryId = resolveExpenseCategory(userId, messages);

        if (resolvedCategoryId == null) {
            throw new BankStatementProcessingException(
                    "No active expense category found for user " + userId
                    + ". Please create at least one active expense category before importing.");
        }

        // ── 3. Parse PDF ─────────────────────────────────────────────────────
        HdfcStatementParserService.StatementParseResult parseResult =
                parserService.parseStatement(file, password);

        List<BankStatementTransactionDTO> transactions = parseResult.getTransactions();
        BigDecimal statementSummaryBalance = parseResult.getSummaryClosingBalance();

        if (transactions.isEmpty()) {
            throw new BankStatementProcessingException(
                    "No transactions could be parsed from the uploaded bank statement. "
                    + "Please ensure this is a valid HDFC Bank account statement.");
        }

        // ── 4. Match user's current closing balance ──────────────────────────
        BigDecimal userClosingBalance = user.getCurrentClosingBalance();
        if (userClosingBalance == null) {
            userClosingBalance = BigDecimal.ZERO;
        }

        int matchIndex = findMatchIndex(transactions, userClosingBalance);

        if (matchIndex < 0) {
            throw new BankStatementProcessingException(
                    "No matching transactions found for the current closing balance ("
                    + userClosingBalance + "). "
                    + "Please upload the latest account statement that contains "
                    + "a transaction with this closing balance.");
        }

        logger.info("Closing balance {} matched at transaction index {} for userId={}",
                userClosingBalance, matchIndex, userId);

        // ── 5. Import transactions after the match point ─────────────────────
        int expensesAdded = 0;
        int incomesAdded  = 0;
        int skippedCount  = 0;

        BigDecimal prevClosing = transactions.get(matchIndex).getClosingBalance();

        for (int i = matchIndex + 1; i < transactions.size(); i++) {
            BankStatementTransactionDTO txn = transactions.get(i);
            BigDecimal currClosing = txn.getClosingBalance();

            if (currClosing == null) {
                messages.add("Skipped transaction at index " + i + " (null closing balance).");
                skippedCount++;
                continue;
            }

            BigDecimal diff = currClosing.subtract(prevClosing);

            if (diff.compareTo(BigDecimal.ZERO) < 0) {
                // Withdrawal → expense
                BigDecimal amount = diff.abs();
                saveExpense(userId, resolvedCategoryId, txn, amount);
                expensesAdded++;
                logger.debug("Added expense: amount={}, narration={}", amount, txn.getNarration());

            } else if (diff.compareTo(BigDecimal.ZERO) > 0) {
                // Deposit → income
                saveIncome(userId, txn, diff);
                incomesAdded++;
                logger.debug("Added income: amount={}, narration={}", diff, txn.getNarration());

            } else {
                // Zero diff – e.g. same-amount debit and credit in one entry (rare)
                messages.add("Skipped transaction on " + txn.getTransactionDate()
                        + " (zero net change): " + truncate(txn.getNarration(), 60));
                skippedCount++;
            }

            prevClosing = currClosing;
        }

        // ── 6. Recalculate closing balance ───────────────────────────────────
        if (expensesAdded > 0 || incomesAdded > 0) {
            closingBalanceService.recalculate(userId);
        }

        // ── 7. Balance reconciliation ────────────────────────────────────────
        // Compare the statement's authoritative closing balance (from STATEMENT SUMMARY)
        // with the user's tracked current_closing_balance.  A mismatch means some
        // transactions may not have been captured or categorised correctly.
        String balanceWarning = null;
        if (statementSummaryBalance != null) {
            BigDecimal trackedBalance = userRepository.findById(userId)
                    .map(u -> u.getCurrentClosingBalance() != null ? u.getCurrentClosingBalance() : BigDecimal.ZERO)
                    .orElse(BigDecimal.ZERO);

            if (trackedBalance.compareTo(statementSummaryBalance) != 0) {
                balanceWarning = String.format(
                        "Closing balance mismatch: the bank statement shows ₹%s but your tracked balance is ₹%s. "
                        + "Please review the imported transactions manually to identify any discrepancies.",
                        statementSummaryBalance.toPlainString(),
                        trackedBalance.toPlainString());
                logger.warn("Balance mismatch for userId={}: statement={}, tracked={}",
                        userId, statementSummaryBalance, trackedBalance);
            } else {
                logger.info("Balance reconciliation OK for userId={}: both = {}", userId, trackedBalance);
            }
        } else {
            logger.warn("STATEMENT SUMMARY not found in PDF for userId={}; skipping balance check.", userId);
        }

        logger.info("Bank statement import complete for userId={}: expenses={}, incomes={}, skipped={}",
                userId, expensesAdded, incomesAdded, skippedCount);

        BankStatementImportResult result = new BankStatementImportResult();
        result.setExpensesAdded(expensesAdded);
        result.setIncomesAdded(incomesAdded);
        result.setSkippedCount(skippedCount);
        result.setMessages(messages);
        result.setStatementClosingBalance(statementSummaryBalance);
        result.setBalanceMatchWarning(balanceWarning);
        return result;
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Resolves the expense category to use for imported expenses.
     *
     * <ol>
     *   <li>Try to find an active category named <em>House Expenses</em>.</li>
     *   <li>If not found, try <em>Miscellaneous</em>.</li>
     *   <li>If still not found, use the first active category (alphabetically).</li>
     * </ol>
     *
     * <p>A human-readable message is appended to {@code messages} describing
     * which category was chosen and a reminder that it can be changed later.
     *
     * @return the resolved category id, or {@code null} if the user has no active categories
     */
    private Integer resolveExpenseCategory(String userId, List<String> messages) {
        // 1. Try "House Expenses"
        Optional<UserExpenseCategory> preferred = categoryRepository
                .findByUserIdAndUserExpenseCategoryNameIgnoreCase(userId, PREFERRED_CATEGORY)
                .filter(c -> "A".equals(c.getStatus()));

        if (preferred.isPresent()) {
            return preferred.get().getUserExpenseCategoryId();
        }

        // 2. Try "Miscellaneous"
        Optional<UserExpenseCategory> fallback = categoryRepository
                .findByUserIdAndUserExpenseCategoryNameIgnoreCase(userId, FALLBACK_CATEGORY)
                .filter(c -> "A".equals(c.getStatus()));

        if (fallback.isPresent()) {
            String catName = fallback.get().getUserExpenseCategoryName();
            messages.add("'" + PREFERRED_CATEGORY + "' category not found. All expenses have been captured under '"
                    + catName + "'. You can change the category for individual expenses later if you want.");
            return fallback.get().getUserExpenseCategoryId();
        }

        // 3. Any active category
        List<UserExpenseCategory> activeCategories = categoryRepository
                .findByUserIdAndStatusOrderByUserExpenseCategoryName(userId, "A");

        if (!activeCategories.isEmpty()) {
            UserExpenseCategory any = activeCategories.get(0);
            messages.add("'" + PREFERRED_CATEGORY + "' category not found. All expenses have been captured under '"
                    + any.getUserExpenseCategoryName() + "'. You can change the category for individual expenses later if you want.");
            return any.getUserExpenseCategoryId();
        }

        return null;
    }

    /**
     * Finds the index of the first transaction whose closing balance equals
     * {@code targetBalance}, using {@link BigDecimal#compareTo} for equality.
     *
     * @return the index, or {@code -1} if not found
     */
    private int findMatchIndex(List<BankStatementTransactionDTO> transactions,
                               BigDecimal targetBalance) {
        for (int i = 0; i < transactions.size(); i++) {
            BigDecimal cb = transactions.get(i).getClosingBalance();
            if (cb != null && cb.compareTo(targetBalance) == 0) {
                return i;
            }
        }
        return -1;
    }

    /** Persists a withdrawal transaction as an {@link Expense}. */
    private void saveExpense(String userId,
                             Integer categoryId,
                             BankStatementTransactionDTO txn,
                             BigDecimal amount) {
        Expense expense = new Expense();
        expense.setUserId(userId);
        expense.setExpenseName(truncate(txn.getNarration(), MAX_NAME_LENGTH));
        expense.setExpenseAmount(amount);
        expense.setUserExpenseCategoryId(categoryId);
        expense.setExpenseDate(txn.getTransactionDate());
        expense.setLastUpdateTmstp(LocalDateTime.now());
        expenseRepository.save(expense);
    }

    /** Persists a deposit transaction as an {@link Income}. */
    private void saveIncome(String userId,
                            BankStatementTransactionDTO txn,
                            BigDecimal amount) {
        LocalDate date = txn.getTransactionDate();
        Income income = new Income();
        income.setUserId(userId);
        income.setSource(truncate(txn.getNarration(), MAX_NAME_LENGTH));
        income.setAmount(amount);
        income.setReceivedDate(date);
        income.setMonth(date.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH).toUpperCase());
        income.setYear(date.getYear());
        income.setLastUpdateTmstp(LocalDateTime.now());
        incomeRepository.save(income);
    }

    /**
     * Truncates {@code text} to at most {@code maxLen} characters.
     * If the text is {@code null} or blank, returns {@code "Bank Statement Import"} as fallback.
     */
    private String truncate(String text, int maxLen) {
        if (text == null || text.isBlank()) return "Bank Statement Import";
        return text.length() <= maxLen ? text : text.substring(0, maxLen);
    }
}




