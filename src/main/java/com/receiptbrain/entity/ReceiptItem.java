package com.receiptbrain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "receipt_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString(exclude = "receipt")
@EqualsAndHashCode(exclude = "receipt")
public class ReceiptItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id")
    private Receipt receipt;

    private String name;
}
