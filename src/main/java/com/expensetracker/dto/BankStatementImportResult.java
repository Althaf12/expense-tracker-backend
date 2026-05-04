package com.expensetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO returned after processing a bank statement import.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BankStatementImportResult {

    /** Number of expense records added (withdrawals). */
    private int expensesAdded;

    /** Number of income records added (deposits). */
    private int incomesAdded;

    /** Number of transactions skipped (zero-amount or unresolvable). */
    private int skippedCount;

    /** Informational / warning messages produced during the import. */
    private List<String> messages;

    /**
     * Closing balance as read from the STATEMENT SUMMARY section of the PDF.
     * {@code null} if the summary section could not be found.
     */
    private BigDecimal statementClosingBalance;

    /**
     * Non-null when the statement's closing balance does not match the user's
     * tracked {@code current_closing_balance} after import.  The frontend should
     * display this as a prominent warning so the user can review transactions manually.
     */
    private String balanceMatchWarning;
}

