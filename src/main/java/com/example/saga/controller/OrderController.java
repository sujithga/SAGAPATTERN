package com.example.saga.controller;

import com.example.saga.dto.CreateOrderRequest;
import com.example.saga.entity.Order;
import com.example.saga.event.OrderCreatedEvent;
import com.example.saga.orchestrator.OrderSagaOrchestrator;
import com.example.saga.repository.OrderRepository;
import com.example.saga.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@Slf4j
@RequiredArgsConstructor
public class OrderController {
    private final OrderSagaOrchestrator orderSagaOrchestrator;
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody CreateOrderRequest request) {
        try {
            log.info("Received order request: Customer ID: {}, Product ID: {}, Quantity: {}",
                    request.getCustomerId(), request.getProductId(), request.getQuantity());

            OrderCreatedEvent event = new OrderCreatedEvent(
                    UUID.randomUUID().getMostSignificantBits(),
                    request.getCustomerId(),
                    request.getProductId(),
                    request.getQuantity(),
                    request.getAmount()
            );

            Order order = orderSagaOrchestrator.startOrderSaga(event);

            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            log.error("Order creation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
            );
        }
    }

    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderRepository.findAll());
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable Long orderId) {
        return orderRepository.findById(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/inventory/stock")
    public ResponseEntity<Map<Long, Integer>> getInventoryStock() {
        return ResponseEntity.ok(inventoryService.getInventory());
    }
}
