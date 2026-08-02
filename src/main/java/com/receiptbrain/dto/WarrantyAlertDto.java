package com.receiptbrain.dto;

import java.time.LocalDate;

public record WarrantyAlertDto(
        Long receiptId,
        String merchant,
        String itemName,
        LocalDate expiresOn,
        long daysRemaining,
        String status
) {}
