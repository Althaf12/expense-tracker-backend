package com.expensetracker.model;

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
@Table(name = "user_preferences", uniqueConstraints = {@UniqueConstraint(columnNames = {"username"})})
public class UserPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_preferences_id")
    private Integer userPreferencesId;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "font_size", length = 1)
    private String fontSize; // S, M, L

    @Column(name = "currency_code", length = 3)
    private String currencyCode; // e.g., INR

    @Column(name = "theme", length = 1)
    private String theme; // D or L

    @Column(name = "last_update_tmstp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastUpdateTmstp;
}
