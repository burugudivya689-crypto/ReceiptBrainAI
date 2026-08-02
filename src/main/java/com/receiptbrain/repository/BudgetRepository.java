package com.receiptbrain.repository;

import com.receiptbrain.entity.Budget;
import com.receiptbrain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUserAndMonth(User user, YearMonth month);
    Optional<Budget> findByUserAndCategoryAndMonth(User user, String category, YearMonth month);
}
