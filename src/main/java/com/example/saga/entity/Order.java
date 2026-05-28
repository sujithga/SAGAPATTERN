package com.example.saga.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderId;
    private Long customerId;
    private Long productId;
    private Integer quantity;
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    public enum OrderStatus {
        PENDING,
        PAYMENT_PROCESSING,
        PAYMENT_COMPLETED,
        INVENTORY_RESERVED,
        ORDER_CONFIRMED,
        PAYMENT_FAILED,
        INVENTORY_FAILED,
        CANCELLED
    }
}
