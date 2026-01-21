package com.expensetracker.admin.model;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "planned_expenses")
public class PlannedExpenses {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "planned_expenses_id")
    private Integer plannedExpensesId;
    @Column(name = "expense_name", length = 100)
    private String expenseName;
    @Column(name = "expense_category", length = 100)
    private String expenseCategory;
    @Column(name = "expense_amount")
    private Double expenseAmount;
    @Column(name = "last_update_tmstp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastUpdateTmstp;
    @PrePersist
    public void prePersist() {
        if (this.lastUpdateTmstp == null) this.lastUpdateTmstp = LocalDateTime.now();
    }
    @PreUpdate
    public void preUpdate() {
        this.lastUpdateTmstp = LocalDateTime.now();
    }
}
