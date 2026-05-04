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
    private final BankStatementPasswordService passwordService;

    @Autowired
    public BankStatementImportService(HdfcStatementParserService parserService,
                                      UserRepository userRepository,
                                      UserExpenseCategoryRepository categoryRepository,
                                      ExpenseRepository expenseRepository,
                                      IncomeRepository incomeRepository,
                                      ClosingBalanceService closingBalanceService,
                                      BankStatementPasswordService passwordService) {
        this.parserService        = parserService;
        this.userRepository       = userRepository;
        this.categoryRepository   = categoryRepository;
        this.expenseRepository    = expenseRepository;
        this.incomeRepository     = incomeRepository;
        this.closingBalanceService = closingBalanceService;
        this.passwordService      = passwordService;
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
     * <h3>Password scenarios</h3>
     * <ol>
     *   <li>{@code useStoredPassword=true}  – decrypt the password stored in DB; fail if none exists.</li>
     *   <li>{@code password} provided + {@code storePassword=true}  – verify the PDF unlocks, then
     *       encrypt and save (replacing any existing stored password).</li>
     *   <li>{@code password} provided + {@code storePassword=false} – use for this import only; do not store.</li>
     *   <li>No password and {@code useStoredPassword=false}  – treat as an unprotected PDF.</li>
     * </ol>
     *
     * @param file              the uploaded PDF file
     * @param userId            the user performing the import
     * @param explicitPassword  plain-text password supplied by the user (may be {@code null})
     * @param useStoredPassword when {@code true} the DB-stored password is used
     * @param storePassword     when {@code true} the supplied {@code explicitPassword} is saved to DB
     * @return summary of what was imported
     */
    @Transactional
    public BankStatementImportResult importStatement(MultipartFile file,
                                                     String userId,
                                                     String explicitPassword,
                                                     boolean useStoredPassword,
                                                     boolean storePassword) {
        // ── 1. Validate user ────────────────────────────────────────────────
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        List<String> messages = new ArrayList<>();

        // ── 2. Resolve the effective PDF password ────────────────────────────
        String effectivePassword = resolvePassword(
                userId, explicitPassword, useStoredPassword, storePassword, file, messages);

        // ── 3. Resolve expense category ──────────────────────────────────────
        Integer resolvedCategoryId = resolveExpenseCategory(userId, messages);
        if (resolvedCategoryId == null) {
            throw new BankStatementProcessingException(
                    "No active expense category found for user " + userId
                    + ". Please create at least one active expense category before importing.");
        }

        // ── 4. Parse PDF ─────────────────────────────────────────────────────
        HdfcStatementParserService.StatementParseResult parseResult =
                parserService.parseStatement(file, effectivePassword);

        List<BankStatementTransactionDTO> transactions = parseResult.getTransactions();
        BigDecimal statementSummaryBalance = parseResult.getSummaryClosingBalance();

        if (transactions.isEmpty()) {
            throw new BankStatementProcessingException(
                    "No transactions could be parsed from the uploaded bank statement. "
                    + "Please ensure this is a valid HDFC Bank account statement.");
        }

        // ── 5. Match user's current closing balance ──────────────────────────
        BigDecimal userClosingBalance = user.getCurrentClosingBalance();
        if (userClosingBalance == null) userClosingBalance = BigDecimal.ZERO;

        int matchIndex = findMatchIndex(transactions, userClosingBalance);
        if (matchIndex < 0) {
            throw new BankStatementProcessingException(
                    "No matching transactions found for the current closing balance ("
                    + userClosingBalance + "). "
                    + "Please upload the latest account statement that contains "
                    + "a transaction with this closing balance.");
        }

        logger.info("Closing balance {} matched at index {} for userId={}", userClosingBalance, matchIndex, userId);

        // ── 6. Import transactions after the match point ─────────────────────
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
                saveExpense(userId, resolvedCategoryId, txn, diff.abs());
                expensesAdded++;
                logger.debug("Added expense: amount={}, narration={}", diff.abs(), txn.getNarration());
            } else if (diff.compareTo(BigDecimal.ZERO) > 0) {
                saveIncome(userId, txn, diff);
                incomesAdded++;
                logger.debug("Added income: amount={}, narration={}", diff, txn.getNarration());
            } else {
                messages.add("Skipped transaction on " + txn.getTransactionDate()
                        + " (zero net change): " + truncate(txn.getNarration(), 60));
                skippedCount++;
            }

            prevClosing = currClosing;
        }

        // ── 7. Recalculate closing balance ───────────────────────────────────
        if (expensesAdded > 0 || incomesAdded > 0) {
            closingBalanceService.recalculate(userId);
        }

        // ── 8. Balance reconciliation ────────────────────────────────────────
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

        logger.info("Import complete for userId={}: expenses={}, incomes={}, skipped={}",
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
    // Password resolution
    // -----------------------------------------------------------------------

    /**
     * Determines the effective PDF password to use — and optionally stores it — based on
     * the combination of flags the caller provided.
     */
    private String resolvePassword(String userId,
                                   String explicitPassword,
                                   boolean useStoredPassword,
                                   boolean storePassword,
                                   MultipartFile file,
                                   List<String> messages) {

        boolean hasExplicit = explicitPassword != null && !explicitPassword.isBlank();
        boolean hasStored   = passwordService.hasStoredPassword(userId);

        // ── Scenario A: use stored password ─────────────────────────────────
        if (useStoredPassword) {
            if (!hasStored) {
                throw new BankStatementProcessingException(
                        "No stored PDF password found for your account. "
                        + "Please provide the password manually.");
            }
            String decrypted = passwordService.getDecryptedPassword(userId);
            messages.add("Using your stored bank statement password to unlock the PDF.");
            logger.info("Using stored PDF password for userId={}", userId);
            return decrypted;
        }

        // ── Scenario B/C: explicit password provided ─────────────────────────
        if (hasExplicit) {
            if (storePassword) {
                // Verify the PDF actually unlocks with this password before storing
                verifyPasswordUnlocksPdf(file, explicitPassword);
                passwordService.storePassword(userId, explicitPassword);
                if (hasStored) {
                    messages.add("Your bank statement password has been updated and saved securely.");
                } else {
                    messages.add("Your bank statement password has been saved securely for future imports.");
                }
                logger.info("PDF password verified and {} for userId={}",
                        hasStored ? "replaced" : "stored", userId);
            } else {
                messages.add("Using the provided password for this import only (not saved).");
            }
            return explicitPassword;
        }

        // ── Scenario D: no password at all ───────────────────────────────────
        if (hasStored) {
            // Inform the user they have a stored password they could use
            messages.add("Note: you have a stored bank statement password. "
                    + "Pass 'useStoredPassword: true' to use it automatically next time.");
        }
        return null; // unprotected PDF
    }

    /**
     * Attempts to open the PDF with the given password purely to validate it.
     * Throws {@link BankStatementProcessingException} if the password is wrong.
     */
    private void verifyPasswordUnlocksPdf(MultipartFile file, String password) {
        try {
            byte[] bytes = file.getBytes();
            org.apache.pdfbox.pdmodel.PDDocument doc =
                    org.apache.pdfbox.pdmodel.PDDocument.load(bytes, password);
            doc.close();
        } catch (org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException e) {
            throw new BankStatementProcessingException(
                    "The provided password is incorrect — it could not unlock the PDF. "
                    + "Password has NOT been saved.");
        } catch (Exception e) {
            throw new BankStatementProcessingException(
                    "Could not verify the PDF password: " + e.getMessage());
        }
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




