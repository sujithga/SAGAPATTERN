package com.example.saga.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFailedEvent extends SagaEvent {
    private String reason;

    public PaymentFailedEvent(Long orderId, Long customerId, String reason) {
        super(orderId, customerId, "PAYMENT_FAILED");
        this.reason = reason;
    }
}
