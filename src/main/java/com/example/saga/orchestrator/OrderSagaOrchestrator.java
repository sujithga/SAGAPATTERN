package com.example.saga.orchestrator;

import com.example.saga.entity.Order;
import com.example.saga.event.*;
import com.example.saga.repository.OrderRepository;
import com.example.saga.service.EventSourcingService;
import com.example.saga.service.InventoryService;
import com.example.saga.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

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

            // Compensation: Rollback all previous steps
            compensateOrderSaga(order, event);

            order.setStatus(Order.OrderStatus.CANCELLED);
            orderRepository.save(order);
            log.info("========== Order Saga Rolled Back ==========");

            throw new RuntimeException("Order saga failed and rolled back: " + e.getMessage());
        }
    }

    private Order createOrder(OrderCreatedEvent event) {
        Order order = new Order();
        order.setOrderId(String.valueOf(UUID.randomUUID()));
        order.setCustomerId(event.getCustomerId());
        order.setProductId(event.getProductId());
        order.setQuantity(event.getQuantity());
        order.setAmount(event.getAmount());
        order.setStatus(Order.OrderStatus.PENDING);

        order = orderRepository.save(order);
        log.info("Order created with ID: {}", order.getId());
        return order;
    }

    private void compensateOrderSaga(Order order, OrderCreatedEvent event) {
        try {
            // Compensation 1: Refund Payment
            if (order.getStatus() == Order.OrderStatus.PAYMENT_COMPLETED ||
                    order.getStatus() == Order.OrderStatus.INVENTORY_RESERVED) {
                log.info("COMPENSATION 1: Refunding payment for order: {}", order.getId());
                PaymentCompletedEvent paymentEvent = new PaymentCompletedEvent(
                        order.getId(), event.getCustomerId(), "TXN-UNKNOWN"
                );
                paymentService.refundPayment(paymentEvent);
                log.info("COMPENSATION 1: Payment refunded");
            }

            // Compensation 2: Release Inventory
            if (order.getStatus() == Order.OrderStatus.INVENTORY_RESERVED) {
                log.info("COMPENSATION 2: Releasing inventory for order: {}", order.getId());
                InventoryReservationEvent inventoryEvent = new InventoryReservationEvent(
                        order.getId(), event.getCustomerId(), event.getProductId(), event.getQuantity()
                );
                inventoryService.releaseInventory(inventoryEvent);
                log.info("COMPENSATION 2: Inventory released");
            }
        } catch (Exception e) {
            log.error("Error during compensation: {}", e.getMessage());
            // In real scenarios, failed compensations should be logged and retried
        }
    }
}
