package com.example.saga.orchestrator;

import com.example.saga.entity.Order;
import com.example.saga.entity.EventStore;
import com.example.saga.event.*;
import com.example.saga.repository.OrderRepository;
import com.example.saga.service.EventSourcingService;
import com.example.saga.service.InventoryService;
import com.example.saga.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * OrderSagaOrchestrator is the core of the saga pattern.
 * It orchestrates the distributed transaction across multiple services.
 *
 * Saga Pattern: A saga is a sequence of local transactions that are coordinated
 * across multiple services. Each local transaction updates the database and
 * publishes events. If any step fails, compensating transactions are triggered
 * to rollback previous steps.
 *
 * Event Sourcing: All events are persisted for an immutable audit trail.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderSagaOrchestrator {
    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final InventoryService inventoryService;
    private final EventSourcingService eventSourcingService;
    private final ObjectMapper objectMapper;

    public Order startOrderSaga(OrderCreatedEvent event) {
        log.info("========== Starting Order Saga for Order ID: {} ==========", event.getOrderId());

        Order order = createOrder(event);
        long version = 1;

        // Persist initial event
        eventSourcingService.appendEvent(order.getId(), event, version++);

        try {
            // Step 1: Process Payment
            log.info("STEP 1: Processing payment...");
            PaymentProcessingEvent paymentEvent = new PaymentProcessingEvent(
                    order.getId(), event.getCustomerId(), event.getAmount()
            );
            PaymentCompletedEvent paymentCompleted = paymentService.processPayment(paymentEvent);
            eventSourcingService.appendEvent(order.getId(), paymentCompleted, version++);

            order.setStatus(Order.OrderStatus.PAYMENT_COMPLETED);
            orderRepository.save(order);
            log.info("STEP 1: Payment completed successfully");

            // Step 2: Reserve Inventory
            log.info("STEP 2: Reserving inventory...");
            InventoryReservationEvent inventoryEvent = new InventoryReservationEvent(
                    order.getId(), event.getCustomerId(), event.getProductId(), event.getQuantity()
            );
            InventoryReservedEvent inventoryReserved = inventoryService.reserveInventory(inventoryEvent);
            eventSourcingService.appendEvent(order.getId(), inventoryReserved, version++);

            order.setStatus(Order.OrderStatus.INVENTORY_RESERVED);
            orderRepository.save(order);
            log.info("STEP 2: Inventory reserved successfully");

            // Step 3: Confirm Order
            log.info("STEP 3: Confirming order...");
            order.setStatus(Order.OrderStatus.ORDER_CONFIRMED);
            orderRepository.save(order);
            log.info("STEP 3: Order confirmed successfully");

            log.info("========== Order Saga Completed Successfully ==========");
            return order;

        } catch (Exception e) {
            log.error("ERROR in Order Saga: {}", e.getMessage());
            log.info("========== Starting Compensation Transactions ==========");

            // Query the Event Store to find out what needs to be compensated
            compensateOrderSaga(order.getId());

            order.setStatus(Order.OrderStatus.CANCELLED);
            orderRepository.save(order);
            log.info("========== Order Saga Rolled Back ==========");

            throw new RuntimeException("Order saga failed and rolled back: " + e.getMessage());
        }
    }

    private Order createOrder(OrderCreatedEvent event) {
        Order order = new Order();
        // Use the ID from the event to ensure the Event Store and Order table are linked correctly
        order.setOrderId(String.valueOf(event.getOrderId()));
        order.setCustomerId(event.getCustomerId());
        order.setProductId(event.getProductId());
        order.setQuantity(event.getQuantity());
        order.setAmount(event.getAmount());
        order.setStatus(Order.OrderStatus.PENDING);

        order = orderRepository.save(order);
        log.info("Order created with ID: {}", order.getId());
        return order;
    }

    /**
     * Uses Event Sourcing history to drive compensation logic.
     * This ensures that even if the system crashed, we know exactly what was done.
     */
    private void compensateOrderSaga(Long orderId) {
        try {
            List<EventStore> history = eventSourcingService.getEventHistory(orderId);
            log.info("Read {} events from history for compensation", history.size());

            // LIFO Compensation: Rollback steps in reverse order

            // 1. Check for Inventory Reserved Event
            findEvent(history, "INVENTORY_RESERVED", InventoryReservedEvent.class).ifPresent(inventoryEvent -> {
                log.info("COMPENSATION: Releasing inventory for order: {}", orderId);
                // We fetch the original request details from the ORDER_CREATED event
                findEvent(history, "ORDER_CREATED", OrderCreatedEvent.class).ifPresent(createdEvent -> {
                    InventoryReservationEvent releaseEvent = new InventoryReservationEvent(
                            orderId, createdEvent.getCustomerId(), createdEvent.getProductId(), createdEvent.getQuantity()
                    );
                    inventoryService.releaseInventory(releaseEvent);
                    log.info("COMPENSATION: Inventory released successfully");
                });
            });

            // 2. Check for Payment Completed Event
            findEvent(history, "PAYMENT_COMPLETED", PaymentCompletedEvent.class).ifPresent(paymentEvent -> {
                log.info("COMPENSATION: Refunding payment for order: {} with TXN: {}", 
                        orderId, paymentEvent.getTransactionId());
                paymentService.refundPayment(paymentEvent);
                log.info("COMPENSATION: Payment refunded successfully");
            });

            // Persist a compensation event for the audit trail
            eventSourcingService.appendEvent(orderId, 
                new CompensationEvent(orderId, null, "SAGA_ROLLBACK", "Failure during orchestration"), 
                (long) history.size() + 1);

        } catch (Exception e) {
            log.error("CRITICAL: Error during compensation for Order {}: {}", orderId, e.getMessage());
        }
    }

    private <T> Optional<T> findEvent(List<EventStore> history, String type, Class<T> clazz) {
        return history.stream()
                .filter(e -> e.getEventType().equals(type))
                .findFirst()
                .map(e -> {
                    try {
                        return objectMapper.readValue(e.getEventData(), clazz);
                    } catch (Exception ex) {
                        log.error("Failed to deserialize event: {}", type);
                        return null;
                    }
                });
    }
}
