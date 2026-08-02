package com.receiptbrain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "receipts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Receipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String merchant;
    private LocalDate purchaseDate;
    private BigDecimal amount;
    private String currency;
    private String category;
    private String paymentMethod;
    private String gstNumber;
    private Integer warrantyMonths;
    private LocalDate warrantyExpiryDate;
    private Integer returnWindowDays;
    private LocalDate returnDeadline;
    private String contentHash;
    private String invoiceNumber;
    private BigDecimal taxAmount;
    private String fileName;
    private String filePath;
    private String previewPath;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String rawText;
    private String aiSummary;
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReceiptItem> items;
}
