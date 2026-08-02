package com.receiptbrain.controller;

import com.receiptbrain.dto.AnalyticsSummaryDto;
import com.receiptbrain.service.ReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final ReceiptService receiptService;

    @GetMapping("/summary")
    public ResponseEntity<AnalyticsSummaryDto> summary(Authentication authentication) {
        return ResponseEntity.ok(receiptService.getAnalyticsSummary(authentication.getName()));
    }
}
