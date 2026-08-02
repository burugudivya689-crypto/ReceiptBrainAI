package com.receiptbrain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.YearMonth;

@Entity
@Table(name = "budgets", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "category", "budget_month"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Budget {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(nullable = false)
    private String category;
    @Column(name = "budget_month", nullable = false)
    private YearMonth month;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal limitAmount;
}
