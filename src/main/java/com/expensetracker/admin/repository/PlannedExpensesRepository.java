package com.expensetracker.admin.repository;
import com.expensetracker.admin.model.PlannedExpenses;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository("adminPlannedExpensesRepository")
public interface PlannedExpensesRepository extends JpaRepository<PlannedExpenses, Integer> {
    List<PlannedExpenses> findAllByOrderByExpenseName();
}
