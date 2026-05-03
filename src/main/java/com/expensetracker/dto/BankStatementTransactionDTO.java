package com.expensetracker.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Internal DTO representing a single transaction parsed from an HDFC bank statement.
 */
@Data
public class BankStatementTransactionDTO {

    /** Transaction date as it appears in the Date column (DD/MM/YY). */
    private LocalDate transactionDate;

    /** Narration / description of the transaction. */
    private String narration;

    /** Cheque / Reference number. */
    private String referenceNo;

    /** Value date from statement. */
    private LocalDate valueDate;

    /**
     * Closing balance after this transaction.
     * Used to determine direction (debit/credit) and amount relative to previous row.
     */
    private BigDecimal closingBalance;
}

