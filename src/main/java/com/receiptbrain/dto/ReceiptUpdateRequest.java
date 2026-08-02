package com.receiptbrain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReceiptUpdateRequest(
        @NotBlank String merchant,
        @NotNull LocalDate purchaseDate,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal amount,
        String currency,
        String category,
        String paymentMethod,
        String gstNumber,
        Integer warrantyMonths
) {}
