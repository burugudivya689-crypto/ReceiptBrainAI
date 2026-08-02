package com.receiptbrain.controller;

import com.receiptbrain.dto.ReceiptDto;
import com.receiptbrain.dto.ReceiptSearchRequest;
import com.receiptbrain.dto.ReceiptUpdateRequest;
import com.receiptbrain.dto.WarrantyAlertDto;
import jakarta.validation.Valid;
import com.receiptbrain.service.ReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/receipts")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;

    @PostMapping("/upload")
    public ResponseEntity<ReceiptDto> upload(@RequestParam("file") MultipartFile file, Authentication authentication) throws IOException {
        return ResponseEntity.ok(receiptService.uploadReceipt(file, authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<List<ReceiptDto>> list(Authentication authentication) {
        return ResponseEntity.ok(receiptService.getUserReceipts(authentication.getName()));
    }

    @PostMapping("/search")
    public ResponseEntity<List<ReceiptDto>> search(@RequestBody ReceiptSearchRequest request, Authentication authentication) {
        return ResponseEntity.ok(receiptService.searchReceipts(authentication.getName(), request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReceiptDto> get(@PathVariable Long id, Authentication authentication) {
        return receiptService.getReceipt(id, authentication.getName())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReceiptDto> update(@PathVariable Long id, @Valid @RequestBody ReceiptUpdateRequest request, Authentication authentication) {
        return receiptService.updateReceipt(id, authentication.getName(), request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/warranties/alerts")
    public ResponseEntity<List<WarrantyAlertDto>> warrantyAlerts(Authentication authentication) {
        return ResponseEntity.ok(receiptService.getWarrantyAlerts(authentication.getName()));
    }
}
