package com.receiptbrain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.YearMonth;

public record BudgetRequest(@NotBlank String category, @NotNull YearMonth month,
                            @NotNull @DecimalMin("0.01") BigDecimal limitAmount) { }
