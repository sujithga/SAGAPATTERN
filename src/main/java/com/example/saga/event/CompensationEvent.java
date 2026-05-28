package com.example.saga.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompensationEvent extends SagaEvent {
    private String compensationType;
    private String reason;

    public CompensationEvent(Long orderId, Long customerId, String compensationType, String reason) {
        super(orderId, customerId, "COMPENSATION");
        this.compensationType = compensationType;
        this.reason = reason;
    }
}
