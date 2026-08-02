package com.receiptbrain.dto;

import java.math.BigDecimal;
import java.time.YearMonth;

public record BudgetDto(Long id, String category, YearMonth month, BigDecimal limitAmount, BigDecimal spent,
                        BigDecimal remaining, int utilizationPercent, boolean overBudget) { }
