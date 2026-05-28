package com.example.saga.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservedEvent extends SagaEvent {
    private String reservationId;

    public InventoryReservedEvent(Long orderId, Long customerId, String reservationId) {
        super(orderId, customerId, "INVENTORY_RESERVED");
        this.reservationId = reservationId;
    }
}
