package com.expensetracker.admin.repository;
import com.expensetracker.admin.model.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository("adminExpenseCategoryRepository")
public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Integer> {
    Optional<ExpenseCategory> findByExpenseCategoryName(String name);
}
