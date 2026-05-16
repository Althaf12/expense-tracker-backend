package com.expensetracker.specification;

import com.expensetracker.model.Expense;
import com.expensetracker.model.UserExpenseCategory;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * JPA Criteria-based specifications for {@link Expense} queries.
 *
 * <p>All predicates are combined with AND via {@link Specification#and}.
 * Any parameter that is {@code null} or blank is simply not added
 * to the specification, so it does not restrict the result set.
 */
public final class ExpenseSpecification {

    private ExpenseSpecification() {}

    /**
     * Builds a composable {@link Specification} from all optional filter parameters.
     *
     * @param userId            required — always applied
     * @param dateStart         optional — earliest expense date (inclusive)
     * @param dateEnd           optional — latest expense date (inclusive)
     * @param filterName        optional — case-insensitive contains on expenseName
     * @param filterCategory    optional — case-insensitive contains on category name
     * @param filterAmountOp    optional — "LT", "EQ", "GT" (requires filterAmountValue)
     * @param filterAmountValue optional — threshold for amount comparison
     * @param filterDateType    optional — "Date", "Month", "Year" (requires filterDateValue)
     * @param filterDateValue   optional — value matching filterDateType granularity
     */
    public static Specification<Expense> build(
            String userId,
            LocalDate dateStart,
            LocalDate dateEnd,
            String filterName,
            String filterCategory,
            String filterAmountOp,
            BigDecimal filterAmountValue,
            String filterDateType,
            String filterDateValue) {

        Specification<Expense> spec = Specification.where(forUserId(userId));

        if (dateStart != null && dateEnd != null) {
            spec = spec.and(betweenDates(dateStart, dateEnd));
        }
        if (filterName != null && !filterName.isBlank()) {
            spec = spec.and(nameContains(filterName));
        }
        if (filterCategory != null && !filterCategory.isBlank()) {
            spec = spec.and(categoryNameContains(filterCategory));
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

    private static Specification<Expense> forUserId(String userId) {
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    private static Specification<Expense> betweenDates(LocalDate start, LocalDate end) {
        return (root, query, cb) -> cb.between(root.get("expenseDate"), start, end);
    }

    private static Specification<Expense> nameContains(String name) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("expenseName")), "%" + name.toLowerCase() + "%");
    }

    /**
     * Filters by category name using a correlated subquery so that the main
     * query's row count is never inflated.
     */
    private static Specification<Expense> categoryNameContains(String categoryName) {
        return (root, query, cb) -> {
            Subquery<Integer> sub = query.subquery(Integer.class);
            Root<UserExpenseCategory> catRoot = sub.from(UserExpenseCategory.class);
            sub.select(catRoot.get("userExpenseCategoryId"))
               .where(cb.like(cb.lower(catRoot.get("userExpenseCategoryName")),
                              "%" + categoryName.toLowerCase() + "%"));
            return root.get("userExpenseCategoryId").in(sub);
        };
    }

    private static Specification<Expense> amountOp(String op, BigDecimal value) {
        return (root, query, cb) -> switch (op.toUpperCase()) {
            case "LT" -> cb.lessThan(root.get("expenseAmount"), value);
            case "GT" -> cb.greaterThan(root.get("expenseAmount"), value);
            case "EQ" -> cb.equal(root.get("expenseAmount"), value);
            default   -> cb.conjunction();
        };
    }

    /** Sub-filters the already-constrained date range by exact date, month number, or year. */
    private static Specification<Expense> dateTypeFilter(String type, String value) {
        return (root, query, cb) -> switch (type.toUpperCase()) {
            case "DATE"  -> cb.equal(root.get("expenseDate"), LocalDate.parse(value));
            case "MONTH" -> cb.equal(
                    cb.function("MONTH", Integer.class, root.get("expenseDate")),
                    Integer.parseInt(value));
            case "YEAR"  -> cb.equal(
                    cb.function("YEAR", Integer.class, root.get("expenseDate")),
                    Integer.parseInt(value));
            default      -> cb.conjunction();
        };
    }
}

