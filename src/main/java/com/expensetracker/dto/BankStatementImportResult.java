package com.expensetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}

