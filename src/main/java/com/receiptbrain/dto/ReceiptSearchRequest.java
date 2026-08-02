package com.receiptbrain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReceiptSearchRequest(String merchant, String category, String query, LocalDate fromDate, LocalDate toDate,
                                   BigDecimal minAmount, BigDecimal maxAmount, String paymentMethod, String warrantyStatus) {}
