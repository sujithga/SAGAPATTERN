package com.example.saga.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class SagaEvent implements Serializable {
    protected Long orderId;
    protected Long customerId;
    protected String eventType;
}
