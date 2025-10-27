package com.expensetracker.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "income")
public class Income {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "income_id")
    private Integer incomeId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "source")
    private String source;

    @Column(name = "amount")
    private Double amount;

    @Column(name = "received_date")
    private LocalDate receivedDate;

    @Column(name = "last_update_tmstp")
    private LocalDateTime lastUpdateTmstp;
}

