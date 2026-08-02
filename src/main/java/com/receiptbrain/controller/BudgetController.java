package com.receiptbrain.controller;

import com.receiptbrain.dto.BudgetDto;
import com.receiptbrain.dto.BudgetRequest;
import com.receiptbrain.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.YearMonth;
import java.util.List;

@RestController @RequestMapping("/api/budgets") @RequiredArgsConstructor
public class BudgetController {
    private final BudgetService budgetService;
    @GetMapping public List<BudgetDto> list(@RequestParam(required = false) String month, Authentication auth) { return budgetService.list(auth.getName(), month == null ? YearMonth.now() : YearMonth.parse(month)); }
    @PostMapping public BudgetDto save(@Valid @RequestBody BudgetRequest request, Authentication auth) { return budgetService.save(auth.getName(), request); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) { return budgetService.delete(auth.getName(), id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build(); }
}
