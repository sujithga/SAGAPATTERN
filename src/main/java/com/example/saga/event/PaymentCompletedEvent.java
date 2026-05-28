package com.example.saga.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent extends SagaEvent {
    private String transactionId;

    public PaymentCompletedEvent(Long orderId, Long customerId, String transactionId) {
        super(orderId, customerId, "PAYMENT_COMPLETED");
        this.transactionId = transactionId;
    }
}
