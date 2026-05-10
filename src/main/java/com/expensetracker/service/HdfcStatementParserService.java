package com.expensetracker.service;

import com.expensetracker.dto.BankStatementTransactionDTO;
import com.expensetracker.exception.BankStatementProcessingException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses HDFC Bank account statement PDFs (password-protected or plain) and
 * returns a {@link StatementParseResult} containing the parsed transactions
 * and the statement-summary closing balance.
 *
 * <h3>HDFC Statement Table Format</h3>
 * <pre>
 * Date | Narration | Chq./Ref.No. | Value Dt | Withdrawal Amt. | Deposit Amt. | Closing Balance
 * </pre>
 * <h3>Multi-page handling</h3>
 * <p>Each page ends with a {@code *Closing balance...} footnote that is NOT a
 * table end — it is just a disclaimer.  The transaction table resumes on the
 * next page after the account-details header.  The only true end marker is
 * {@code STATEMENT SUMMARY}.</p>
 */
@Service
public class HdfcStatementParserService {

    // -----------------------------------------------------------------------
    // Result wrapper
    // -----------------------------------------------------------------------

    /**
     * Wraps the list of parsed transactions together with the closing balance
     * extracted from the {@code STATEMENT SUMMARY} section of the PDF.
     */
    public static class StatementParseResult {
        private final List<BankStatementTransactionDTO> transactions;
        private final BigDecimal summaryClosingBalance;

        public StatementParseResult(List<BankStatementTransactionDTO> transactions,
                                    BigDecimal summaryClosingBalance) {
            this.transactions          = transactions;
            this.summaryClosingBalance = summaryClosingBalance;
        }

        public List<BankStatementTransactionDTO> getTransactions() { return transactions; }
        /** May be {@code null} if the summary section was not found in the PDF. */
        public BigDecimal getSummaryClosingBalance() { return summaryClosingBalance; }
    }

    private static final Logger logger = LoggerFactory.getLogger(HdfcStatementParserService.class);

    /** HDFC statements use DD/MM/YY (2-digit year, interpreted as 2000-2099). */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yy");

    /** Matches the start of a transaction: a line beginning with a date. */
    private static final Pattern TRANSACTION_START = Pattern.compile("^(\\d{2}/\\d{2}/\\d{2})\\s+");

    /**
     * HDFC Chq./Ref.No. is always exactly 16 decimal digits (zero-padded).
     * The negative look-around ensures we do not match a 16-digit run inside
     * a longer digit sequence (e.g. account numbers > 16 digits).
     */
    private static final Pattern REF_NO_PATTERN = Pattern.compile("(?<!\\d)(\\d{16})(?!\\d)");

    /**
     * Fallback ref-no pattern for cases where PDFBox inserts a column-alignment
     * space in the middle of the 16-digit reference number (e.g.
     * {@code "00005448 55685738"}).  Only matches sequences that start with
     * {@code 0} (HDFC reference numbers are always zero-prefixed) to minimise
     * false positives against phone numbers / UPI IDs in the narration.
     */
    private static final Pattern REF_NO_SPLIT_PATTERN =
            Pattern.compile("(?<!\\d)(0\\d{7})\\s(\\d{8})(?!\\d)");

    /** Matches monetary amounts in the form {@code 1,23,456.78} or {@code 270.00}. */
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("([\\d,]+\\.\\d{2})");

    /** Value-date pattern (same DD/MM/YY). */
    private static final Pattern VALUE_DATE_PATTERN = Pattern.compile("^(\\d{2}/\\d{2}/\\d{2})\\s*");

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Parse the uploaded PDF bank statement.
     *
     * @param file     the multipart PDF file
     * @param password the PDF owner/user password (may be {@code null} or blank for unprotected PDFs)
     * @return {@link StatementParseResult} with transactions (oldest first) and summary closing balance
     */
    public StatementParseResult parseStatement(MultipartFile file, String password) {
        if (file == null || file.isEmpty()) {
            throw new BankStatementProcessingException("Bank statement file must not be empty.");
        }

        try {
            byte[] bytes = file.getBytes();
            PDDocument doc;
            try {
                if (password != null && !password.isBlank()) {
                    doc = PDDocument.load(bytes, password);
                } else {
                    doc = PDDocument.load(bytes);
                }
            } catch (org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException e) {
                throw new BankStatementProcessingException(
                        "Invalid PDF password. Please provide the correct password for the bank statement.", e);
            }

            try {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                String text = stripper.getText(doc);
                logger.debug("Extracted {} characters from bank statement PDF", text.length());
                return parseTransactions(text);
            } finally {
                doc.close();
            }

        } catch (BankStatementProcessingException e) {
            throw e;
        } catch (Exception e) {
            throw new BankStatementProcessingException(
                    "Failed to process bank statement PDF: " + e.getMessage(), e);
        }
    }

    // -----------------------------------------------------------------------
    // Parsing helpers
    // -----------------------------------------------------------------------

    /**
     * Splits the raw extracted text into individual transaction blocks and
     * parses each block into a {@link BankStatementTransactionDTO}.
     *
     * <h3>Multi-page support</h3>
     * <ul>
     *   <li>The {@code *Closing balance...} footnote at the end of each page is
     *       treated as a page-break marker: the current block is flushed and
     *       {@code inTable} is set to {@code false}.  When the next page's column
     *       header row (containing "Closing Balance") is encountered, {@code inTable}
     *       becomes {@code true} again and parsing continues.</li>
     *   <li>The {@code STATEMENT SUMMARY} line is the only true end-of-transactions
     *       marker.  The amounts on the following lines are read to capture the
     *       authoritative closing balance for balance-reconciliation.</li>
     * </ul>
     */
    private StatementParseResult parseTransactions(String text) {
        List<BankStatementTransactionDTO> transactions = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");

        boolean      inTable               = false;
        boolean      inSummary             = false;
        List<String> currentBlock          = new ArrayList<>();
        BigDecimal   summaryClosingBalance  = null;

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;

            // ── STATEMENT SUMMARY section: read until we find the amounts row ──
            if (inSummary) {
                List<BigDecimal> rowAmounts = extractAllAmounts(line);
                if (!rowAmounts.isEmpty()) {
                    // The last amount on the values row is the summary closing balance
                    summaryClosingBalance = rowAmounts.get(rowAmounts.size() - 1);
                    logger.info("STATEMENT SUMMARY closing balance = {}", summaryClosingBalance);
                    break; // Nothing left to parse after this
                }
                // Still on the header labels row (Opening Balance, Dr Count…) — continue
                continue;
            }

            // ── STATEMENT SUMMARY start: flush last block, then enter summary mode ──
            if (line.startsWith("STATEMENT SUMMARY")) {
                if (!currentBlock.isEmpty()) {
                    parseSingleBlock(currentBlock, transactions);
                    currentBlock.clear();
                }
                inTable   = false;
                inSummary = true;
                continue;
            }

            // ── Page-break footnote ("*Closing balance includes funds earmarked…")
            //    This is a DISCLAIMER, not an end-of-transactions marker.
            //    Flush the current block and pause the table until the next page header. ──
            if (line.startsWith("*Closing balance") || line.startsWith("*Funds")) {
                if (!currentBlock.isEmpty()) {
                    parseSingleBlock(currentBlock, transactions);
                    currentBlock.clear();
                }
                inTable = false;
                continue;
            }

            // ── Generated On = hard stop (footer metadata, never mid-table) ──
            if (line.startsWith("Generated On")) {
                if (!currentBlock.isEmpty()) {
                    parseSingleBlock(currentBlock, transactions);
                    currentBlock.clear();
                }
                break;
            }

            if (!inTable) {
                // Re-detect table start on each page: column header contains "Closing Balance"
                if (line.contains("Closing Balance")) {
                    inTable = true;
                }
                // A transaction line can also open the table directly
                if (TRANSACTION_START.matcher(line).find()) {
                    inTable = true;
                    currentBlock.add(line);
                }
                continue;
            }

            // ── New transaction block starts with DD/MM/YY ─────────────────
            if (TRANSACTION_START.matcher(line).find()) {
                if (!currentBlock.isEmpty()) {
                    parseSingleBlock(currentBlock, transactions);
                    currentBlock.clear();
                }
                currentBlock.add(line);
            } else if (!currentBlock.isEmpty()) {
                // Narration overflow / continuation line
                currentBlock.add(line);
            }
        }

        // Flush any remaining pending block
        if (!currentBlock.isEmpty()) {
            parseSingleBlock(currentBlock, transactions);
        }

        logger.info("Parsed {} transactions from HDFC bank statement. Summary closing balance: {}",
                transactions.size(), summaryClosingBalance);
        return new StatementParseResult(transactions, summaryClosingBalance);
    }

    /** Extracts all {@code x,xx,xxx.xx}-style amounts from a line. */
    private List<BigDecimal> extractAllAmounts(String line) {
        List<BigDecimal> amounts = new ArrayList<>();
        Matcher m = AMOUNT_PATTERN.matcher(line);
        while (m.find()) {
            BigDecimal amt = parseAmount(m.group(1));
            if (amt != null) amounts.add(amt);
        }
        return amounts;
    }

    /**
     * Parses a single transaction block (one or more consecutive lines belonging
     * to the same transaction) into a {@link BankStatementTransactionDTO}.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Join all block lines with a space.</li>
     *   <li>Extract the transaction date from the start.</li>
     *   <li>Find the 16-digit reference number (strict, or 8+8 split by a space).</li>
     *   <li>Extract the value date immediately after the ref number.</li>
     *   <li>Scan only the <em>leading</em> whitespace-separated tokens of the remaining
     *       text for monetary amounts; stop at the first non-amount token to avoid
     *       picking up decimal-like strings inside UPI IDs or continuation narration.</li>
     *   <li>Last amount found = closing balance for this transaction.</li>
     * </ol>
     */
    private void parseSingleBlock(List<String> blockLines,
                                   List<BankStatementTransactionDTO> out) {
        String block = String.join(" ", blockLines).trim();

        // 1. Transaction date
        Matcher startMatcher = TRANSACTION_START.matcher(block);
        if (!startMatcher.find()) {
            logger.debug("Skipping block (no leading date): {}", block);
            return;
        }
        String dateStr = startMatcher.group(1);
        int afterDatePos = startMatcher.end();

        // 2. Reference number (16 digits, consecutive or split by one space due to PDF column rendering)
        Matcher refMatcher = REF_NO_PATTERN.matcher(block);
        String refNo;
        int refStart, refEnd;
        if (refMatcher.find()) {
            refNo    = refMatcher.group(1);
            refStart = refMatcher.start();
            refEnd   = refMatcher.end();
        } else {
            // PDFBox sometimes inserts a column-alignment space in the 16-digit ref:
            // e.g. "00005448 55685738" → strip space → "0000544855685738"
            Matcher splitRef = REF_NO_SPLIT_PATTERN.matcher(block);
            if (!splitRef.find()) {
                logger.debug("Skipping block (no 16-digit ref no): {}", block);
                return;
            }
            refNo    = splitRef.group(1) + splitRef.group(2);
            refStart = splitRef.start();
            refEnd   = splitRef.end();
        }

        // 3. Narration = text between end-of-date and start-of-ref-no
        String narration = block.substring(afterDatePos, refStart).trim();
        // Sanitise: collapse multiple spaces inside narration
        narration = narration.replaceAll("\\s{2,}", " ");

        // 4. Value date (DD/MM/YY immediately after ref no)
        String postRef = block.substring(refEnd).trim();
        String valueDateStr = null;
        String amountsSection = postRef;

        Matcher valueDateMatcher = VALUE_DATE_PATTERN.matcher(postRef);
        if (valueDateMatcher.find()) {
            valueDateStr = valueDateMatcher.group(1);
            amountsSection = postRef.substring(valueDateMatcher.end()).trim();
        }

        // 5. Extract monetary amounts from the LEADING tokens of the amounts section only.
        //
        //    HDFC table format after the value date:
        //      <Withdrawal Amt>  <Deposit Amt>  <Closing Balance>
        //    Only one of Withdrawal / Deposit is present per transaction; the blank
        //    column is simply absent in the extracted text.  So there are always
        //    1–3 consecutive amount tokens before any narration-continuation text.
        //
        //    IMPORTANT: scanning the entire section with a regex find() loop would
        //    accidentally pick up decimal-like strings embedded in UPI IDs and phone
        //    numbers carried in continuation lines (e.g. "99-MARKET99.60998088@HDFC"
        //    → "99.60").  We therefore stop as soon as a token does not fully match
        //    the amount pattern, so that continuation narration is never reached.
        List<BigDecimal> amounts = new ArrayList<>();
        String[] amountTokens = amountsSection.trim().split("\\s+");
        for (String token : amountTokens) {
            if (token.isEmpty()) continue;
            // Use matches() so the ENTIRE token must be a valid amount (no prefix/suffix)
            if (AMOUNT_PATTERN.matcher(token).matches()) {
                BigDecimal amt = parseAmount(token);
                if (amt != null) amounts.add(amt);
            } else {
                // First non-amount token signals the start of narration continuation — stop here
                break;
            }
        }

        if (amounts.isEmpty()) {
            logger.debug("Skipping block (no amounts found): {}", block);
            return;
        }

        // Last amount = closing balance
        BigDecimal closingBalance = amounts.get(amounts.size() - 1);

        // Parse dates
        LocalDate txnDate;
        LocalDate valueDate;
        try {
            txnDate  = LocalDate.parse(dateStr, DATE_FORMATTER);
            valueDate = (valueDateStr != null)
                    ? LocalDate.parse(valueDateStr, DATE_FORMATTER)
                    : txnDate;
        } catch (Exception e) {
            logger.warn("Skipping block due to date parse error (dateStr='{}'): {}", dateStr, e.getMessage());
            return;
        }

        BankStatementTransactionDTO txn = new BankStatementTransactionDTO();
        txn.setTransactionDate(txnDate);
        txn.setValueDate(valueDate);
        txn.setNarration(narration);
        txn.setReferenceNo(refNo);
        txn.setClosingBalance(closingBalance);

        out.add(txn);
    }

    /** Parses a comma-formatted amount string (e.g. {@code "1,23,456.78"}) to {@link BigDecimal}. */
    private BigDecimal parseAmount(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return new BigDecimal(s.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

