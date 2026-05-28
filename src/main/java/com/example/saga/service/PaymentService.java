package com.example.saga.service;

import com.example.saga.event.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Slf4j
public class PaymentService {

    public PaymentCompletedEvent processPayment(PaymentProcessingEvent event) {
        log.info("Processing payment for order: {} with amount: {}", event.getOrderId(), event.getAmount());

        // Simulate payment processing
        try {
            // In real scenario, this would call an external payment gateway
            if (event.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Invalid payment amount");
            }

            Thread.sleep(1000); // Simulate processing time
            String transactionId = "TXN-" + UUID.randomUUID();

            log.info("Payment successful for order: {} with transaction ID: {}", event.getOrderId(), transactionId);
            return new PaymentCompletedEvent(event.getOrderId(), event.getCustomerId(), transactionId);
        } catch (Exception e) {
            log.error("Payment failed for order: {}", event.getOrderId(), e);
            throw new RuntimeException("Payment processing failed: " + e.getMessage());
        }
    }

    public void refundPayment(PaymentCompletedEvent paymentEvent) {
        log.info("Refunding payment for order: {} with transaction ID: {}",
                paymentEvent.getOrderId(), paymentEvent.getTransactionId());

        // Simulate refund processing
        try {
            Thread.sleep(500);
            log.info("Refund successful for transaction: {}", paymentEvent.getTransactionId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Refund processing interrupted", e);
        }
    }
}
