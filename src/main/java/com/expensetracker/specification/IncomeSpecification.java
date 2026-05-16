package com.expensetracker.specification;

import com.expensetracker.model.Income;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * JPA Criteria-based specifications for {@link Income} queries.
 */
public final class IncomeSpecification {

    private IncomeSpecification() {}

    /**
     * Builds a composable {@link Specification} from all optional filter parameters.
     *
     * @param userId            required — always applied
     * @param dateStart         optional — earliest received date (inclusive)
     * @param dateEnd           optional — latest received date (inclusive)
     * @param filterSource      optional — case-insensitive contains on source
     * @param filterAmountOp    optional — "LT", "EQ", "GT" (requires filterAmountValue)
     * @param filterAmountValue optional — threshold for amount comparison
     * @param filterDateType    optional — "Date", "Month", "Year" (requires filterDateValue)
     * @param filterDateValue   optional — value matching filterDateType granularity
     */
    public static Specification<Income> build(
            String userId,
            LocalDate dateStart,
            LocalDate dateEnd,
            String filterSource,
            String filterAmountOp,
            BigDecimal filterAmountValue,
            String filterDateType,
            String filterDateValue) {

        Specification<Income> spec = Specification.where(forUserId(userId));

        if (dateStart != null && dateEnd != null) {
            spec = spec.and(betweenDates(dateStart, dateEnd));
        }
        if (filterSource != null && !filterSource.isBlank()) {
            spec = spec.and(sourceContains(filterSource));
        }
        if (filterAmountOp != null && filterAmountValue != null) {
            spec = spec.and(amountOp(filterAmountOp, filterAmountValue));
        }
        if (filterDateType != null && filterDateValue != null && !filterDateValue.isBlank()) {
            spec = spec.and(dateTypeFilter(filterDateType, filterDateValue));
        }

        return spec;
    }

    // ── Individual predicates ─────────────────────────────────────────────

    private static Specification<Income> forUserId(String userId) {
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    private static Specification<Income> betweenDates(LocalDate start, LocalDate end) {
        return (root, query, cb) -> cb.between(root.get("receivedDate"), start, end);
    }

    private static Specification<Income> sourceContains(String source) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("source")), "%" + source.toLowerCase() + "%");
    }

    private static Specification<Income> amountOp(String op, BigDecimal value) {
        return (root, query, cb) -> switch (op.toUpperCase()) {
            case "LT" -> cb.lessThan(root.get("amount"), value);
            case "GT" -> cb.greaterThan(root.get("amount"), value);
            case "EQ" -> cb.equal(root.get("amount"), value);
            default   -> cb.conjunction();
        };
    }

    /** Sub-filters the already-constrained date range by exact date, month number, or year. */
    private static Specification<Income> dateTypeFilter(String type, String value) {
        return (root, query, cb) -> switch (type.toUpperCase()) {
            case "DATE"  -> cb.equal(root.get("receivedDate"), LocalDate.parse(value));
            case "MONTH" -> cb.equal(
                    cb.function("MONTH", Integer.class, root.get("receivedDate")),
                    Integer.parseInt(value));
            case "YEAR"  -> cb.equal(
                    cb.function("YEAR", Integer.class, root.get("receivedDate")),
                    Integer.parseInt(value));
            default      -> cb.conjunction();
        };
    }
}

