package com.receiptbrain.repository;

import com.receiptbrain.entity.Receipt;
import com.receiptbrain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, Long> {
    List<Receipt> findByUser(User user);
    boolean existsByUserAndContentHash(User user, String contentHash);
}
