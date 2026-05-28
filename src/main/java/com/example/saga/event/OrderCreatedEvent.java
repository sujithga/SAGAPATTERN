package com.example.saga.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent extends SagaEvent {
    private Long productId;
    private Integer quantity;
    private BigDecimal amount;

    public OrderCreatedEvent(Long orderId, Long customerId, Long productId, Integer quantity, BigDecimal amount) {
        super(orderId, customerId, "ORDER_CREATED");
        this.productId = productId;
        this.quantity = quantity;
        this.amount = amount;
    }
}
