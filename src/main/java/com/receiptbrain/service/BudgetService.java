package com.receiptbrain.service;

import com.receiptbrain.dto.BudgetDto;
import com.receiptbrain.dto.BudgetRequest;
import com.receiptbrain.entity.Budget;
import com.receiptbrain.entity.Receipt;
import com.receiptbrain.entity.User;
import com.receiptbrain.repository.BudgetRepository;
import com.receiptbrain.repository.ReceiptRepository;
import com.receiptbrain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;

@Service @RequiredArgsConstructor
public class BudgetService {
    private final BudgetRepository budgetRepository; private final ReceiptRepository receiptRepository; private final UserRepository userRepository;
    public BudgetDto save(String email, BudgetRequest request) {
        User user = userRepository.findByEmail(email).orElseThrow();
        Budget budget = budgetRepository.findByUserAndCategoryAndMonth(user, request.category().trim(), request.month()).orElseGet(Budget::new);
        budget.setUser(user); budget.setCategory(request.category().trim()); budget.setMonth(request.month()); budget.setLimitAmount(request.limitAmount());
        return toDto(budgetRepository.save(budget), user);
    }
    public List<BudgetDto> list(String email, YearMonth month) { User user = userRepository.findByEmail(email).orElseThrow(); return budgetRepository.findByUserAndMonth(user, month).stream().map(b -> toDto(b, user)).toList(); }
    public boolean delete(String email, Long id) { User user = userRepository.findByEmail(email).orElseThrow(); return budgetRepository.findById(id).filter(b -> b.getUser().getId().equals(user.getId())).map(b -> { budgetRepository.delete(b); return true; }).orElse(false); }
    private BudgetDto toDto(Budget budget, User user) {
        BigDecimal spent = receiptRepository.findByUser(user).stream().filter(r -> budget.getCategory().equalsIgnoreCase(r.getCategory())).filter(r -> r.getPurchaseDate() != null && YearMonth.from(r.getPurchaseDate()).equals(budget.getMonth())).map(Receipt::getAmount).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remaining = budget.getLimitAmount().subtract(spent);
        int percent = budget.getLimitAmount().signum() == 0 ? 0 : spent.multiply(BigDecimal.valueOf(100)).divide(budget.getLimitAmount(), 0, RoundingMode.HALF_UP).intValue();
        return new BudgetDto(budget.getId(), budget.getCategory(), budget.getMonth(), budget.getLimitAmount(), spent, remaining, percent, spent.compareTo(budget.getLimitAmount()) > 0);
    }
}
