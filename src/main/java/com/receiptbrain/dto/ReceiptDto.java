package com.receiptbrain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReceiptDto(
        Long id,
        String merchant,
        LocalDate purchaseDate,
        BigDecimal amount,
        String currency,
        String category,
        String paymentMethod,
        String gstNumber,
        Integer warrantyMonths,
        LocalDate warrantyExpiryDate,
        String fileName,
        String aiSummary,
        LocalDateTime createdAt
) {}
