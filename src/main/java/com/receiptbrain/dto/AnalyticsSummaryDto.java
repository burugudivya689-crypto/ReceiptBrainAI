package com.receiptbrain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AnalyticsSummaryDto(
        long totalReceipts,
        BigDecimal totalSpending,
        BigDecimal averageReceipt,
        BigDecimal highestExpense,
        List<Breakdown> categoryBreakdown,
        List<Breakdown> merchantBreakdown,
        List<RecentReceiptDto> recentReceipts
) {
    public record Breakdown(String label, BigDecimal total) {}

    public record RecentReceiptDto(Long id, String merchant, BigDecimal amount, String category, LocalDate purchaseDate) {}
}
