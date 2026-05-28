package com.example.saga;

import com.example.saga.entity.Order;
import com.example.saga.event.OrderCreatedEvent;
import com.example.saga.orchestrator.OrderSagaOrchestrator;
import com.example.saga.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OrderSagaOrchestratorTest {

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    @Autowired
    private OrderSagaOrchestrator orderSagaOrchestrator;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void testSuccessfulOrderSaga() {
        // Arrange
        OrderCreatedEvent event = new OrderCreatedEvent(
                1L,
                100L,
                1L,
                5,
                new BigDecimal("100.00")
        );

        // Act
        Order result = orderSagaOrchestrator.startOrderSaga(event);

        // Assert
        assertNotNull(result);
        assertEquals(Order.OrderStatus.ORDER_CONFIRMED, result.getStatus());
        assertTrue(orderRepository.existsById(result.getId()));
    }

    @Test
    void testOrderSagaWithInsufficientInventory() {
        // Arrange - requesting more inventory than available
        OrderCreatedEvent event = new OrderCreatedEvent(
                2L,
                101L,
                1L,
                500, // Product 1 only has 100 units
                new BigDecimal("5000.00")
        );

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            orderSagaOrchestrator.startOrderSaga(event);
        });

        // Verify order is in cancelled state
        Order order = orderRepository.findById(2L).orElse(null);
        if (order != null) {
            assertEquals(Order.OrderStatus.CANCELLED, order.getStatus());
        }
    }

    @Test
    void testOrderSagaWithInvalidPaymentAmount() {
        // Arrange - invalid payment amount
        OrderCreatedEvent event = new OrderCreatedEvent(
                3L,
                102L,
                1L,
                5,
                new BigDecimal("-100.00") // Invalid negative amount
        );

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            orderSagaOrchestrator.startOrderSaga(event);
        });
    }

    @Test
    void testMultipleOrderProcessing() {
        // Arrange
        OrderCreatedEvent event1 = new OrderCreatedEvent(
                4L,
                103L,
                1L,
                2,
                new BigDecimal("50.00")
        );

        OrderCreatedEvent event2 = new OrderCreatedEvent(
                5L,
                104L,
                2L,
                3,
                new BigDecimal("75.00")
        );

        // Act
        Order order1 = orderSagaOrchestrator.startOrderSaga(event1);
        Order order2 = orderSagaOrchestrator.startOrderSaga(event2);

        // Assert
        assertEquals(Order.OrderStatus.ORDER_CONFIRMED, order1.getStatus());
        assertEquals(Order.OrderStatus.ORDER_CONFIRMED, order2.getStatus());
        assertEquals(2, orderRepository.count());
    }
}
