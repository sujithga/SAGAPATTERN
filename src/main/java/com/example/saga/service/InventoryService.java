package com.example.saga.service;

import com.example.saga.event.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class InventoryService {
    // Mock inventory stock
    private static final Map<Long, Integer> inventory = new HashMap<>();

    static {
        inventory.put(1L, 100);
        inventory.put(2L, 50);
        inventory.put(3L, 75);
    }

    public InventoryReservedEvent reserveInventory(InventoryReservationEvent event) {
        log.info("Reserving inventory for order: {} - Product: {}, Quantity: {}",
                event.getOrderId(), event.getProductId(), event.getQuantity());

        try {
            synchronized (inventory) {
                Integer currentStock = inventory.getOrDefault(event.getProductId(), 0);

                if (currentStock < event.getQuantity()) {
                    throw new IllegalArgumentException(
                            "Insufficient inventory. Available: " + currentStock + ", Requested: " + event.getQuantity()
                    );
                }

                // Reserve inventory
                inventory.put(event.getProductId(), currentStock - event.getQuantity());
                Thread.sleep(800); // Simulate processing time

                String reservationId = "RES-" + UUID.randomUUID();
                log.info("Inventory reserved for order: {} with reservation ID: {}", event.getOrderId(), reservationId);
                return new InventoryReservedEvent(event.getOrderId(), event.getCustomerId(), reservationId);
            }
        } catch (Exception e) {
            log.error("Inventory reservation failed for order: {}", event.getOrderId(), e);
            throw new RuntimeException("Inventory reservation failed: " + e.getMessage());
        }
    }

    public void releaseInventory(InventoryReservationEvent inventoryEvent) {
        log.info("Releasing inventory for order: {} - Product: {}, Quantity: {}",
                inventoryEvent.getOrderId(), inventoryEvent.getProductId(), inventoryEvent.getQuantity());

        try {
            synchronized (inventory) {
                Integer currentStock = inventory.getOrDefault(inventoryEvent.getProductId(), 0);
                inventory.put(inventoryEvent.getProductId(), currentStock + inventoryEvent.getQuantity());
                Thread.sleep(500);
                log.info("Inventory released for order: {}", inventoryEvent.getOrderId());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Inventory release interrupted", e);
        }
    }

    public Map<Long, Integer> getInventory() {
        return new HashMap<>(inventory);
    }
}
