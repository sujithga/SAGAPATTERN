package com.example.saga.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservationEvent extends SagaEvent {
    private Long productId;
    private Integer quantity;

    public InventoryReservationEvent(Long orderId, Long customerId, Long productId, Integer quantity) {
        super(orderId, customerId, "INVENTORY_RESERVATION");
        this.productId = productId;
        this.quantity = quantity;
    }
}
