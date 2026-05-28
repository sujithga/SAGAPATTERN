package com.example.saga.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProcessingEvent extends SagaEvent {
    private BigDecimal amount;

    public PaymentProcessingEvent(Long orderId, Long customerId, BigDecimal amount) {
        super(orderId, customerId, "PAYMENT_PROCESSING");
        this.amount = amount;
    }
}
