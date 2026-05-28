package com.example.saga.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservationFailedEvent extends SagaEvent {
    private String reason;

    public InventoryReservationFailedEvent(Long orderId, Long customerId, String reason) {
        super(orderId, customerId, "INVENTORY_RESERVATION_FAILED");
        this.reason = reason;
    }
}
