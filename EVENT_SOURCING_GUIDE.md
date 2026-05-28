# Event Sourcing Guide

## What is Event Sourcing?

Event sourcing is an architectural pattern where all changes to application state are captured as a sequence of immutable events. Instead of storing just the current state, you store every event that happened to create that state.

### Traditional Approach vs Event Sourcing

**Traditional:**
```
Order Table:
| ID | Status | Amount | ... |
| 1  | CONFIRMED | 100.00 | ... |
```
*Only current state is stored. History is lost.*

**Event Sourcing:**
```
Event Store:
| ID | OrderID | EventType | EventData | Version | CreatedAt |
| 1  | 1       | ORDER_CREATED | {...} | 1 | 2026-05-28 |
| 2  | 1       | PAYMENT_COMPLETED | {...} | 2 | 2026-05-28 |
| 3  | 1       | INVENTORY_RESERVED | {...} | 3 | 2026-05-28 |
| 4  | 1       | ORDER_CONFIRMED | {...} | 4 | 2026-05-28 |
```
*Complete history is preserved. State derived from events.*

## Why Event Sourcing?

### 1. **Audit Trail**
Every action is recorded immutably. Perfect for compliance and debugging.

```
Order #123 Timeline:
- 2:00 PM: Order created by Customer 456
- 2:05 PM: Payment processed ($100.00)
- 2:07 PM: Inventory reserved (5 units of Product 1)
- 2:08 PM: Order confirmed
```

### 2. **Temporal Queries**
Reconstruct state at any point in time.

```
What was the order status at 2:06 PM?
→ Search events up to 2:06 PM
→ Answer: PAYMENT_COMPLETED (inventory not yet reserved)
```

### 3. **Event Replay**
Replay events to understand how you reached current state.

```
Debugging: Why is inventory now 75 instead of 100?
→ Replay events for Product 1
→ Event 1: Initial stock 100
→ Event 2: Order #123 reserved 5 → 95
→ Event 3: Order #124 reserved 10 → 85
→ Event 4: Refund for Order #123 → 90
→ ... etc
```

### 4. **Consistency with Saga Pattern**
Perfect match: Sagas are event-driven, events are sourced.

```
Saga step → Event created → Event sourced → State updated
```

### 5. **Scalability (CQRS)**
Separate read and write models using events.

```
Write Model (Command Side):
- Processes commands
- Generates events
- Persists to Event Store

Read Model (Query Side):
- Subscribes to events
- Updates read-optimized database
- Serves fast queries
```

## Implementation in Your Project

### 1. **EventStore Entity**
```java
@Entity
public class EventStore {
    private Long orderId;
    private String eventType;        // "PAYMENT_COMPLETED"
    private String eventData;        // Serialized JSON
    private Long aggregateVersion;   // Version 1, 2, 3...
    private LocalDateTime createdAt;
}
```

### 2. **EventSourcingService**
Persists events when they occur:
```java
eventSourcingService.appendEvent(orderId, paymentEvent, version);
```

### 3. **Integration with Saga**
OrderSagaOrchestrator now captures every step:

```
Order Creation
  ↓ (version 1)
ORDER_CREATED event persisted
  ↓
Payment Processing
  ↓ (version 2)
PAYMENT_COMPLETED event persisted
  ↓
Inventory Reservation
  ↓ (version 3)
INVENTORY_RESERVED event persisted
  ↓
Order Confirmation
  ↓ (version 4)
ORDER_CONFIRMED event persisted (derived state)
```

### 4. **Query Event History**

**Via REST API:**
```bash
# Get all events for an order
GET /api/events/order/123

# Get all events across all orders
GET /api/events/all
```

**Via Service:**
```java
List<EventStore> history = eventSourcingService.getEventHistory(123);
```

## Key Design Principles

### 1. **Immutability**
Events are never modified or deleted. Only new events are added.

```java
// ✓ Correct
eventStore.setEventData(newData);  // Never do this!

// ✓ Correct
eventSourcingService.appendEvent(...);  // Always append
```

### 2. **Idempotency** (Critical for Sagas)
If an event is processed twice, it should have the same effect.

```java
// Idempotent: Safe to call multiple times
processPayment(paymentId, amount) {
    // Check if already processed
    if (isAlreadyProcessed(paymentId)) {
        return getExistingResult(paymentId);
    }
    // Process only once
    process();
}
```

### 3. **Versioning**
Each event has an aggregate version to detect conflicts.

```
Version 1: ORDER_CREATED
Version 2: PAYMENT_COMPLETED
Version 3: INVENTORY_RESERVED
Version 4: ORDER_CONFIRMED

If you replay versions 1-3, you get state at that point in time.
```

### 4. **Causality**
Events must be ordered (which they are via timestamps/versions).

```
Event A (timestamp 2:00) must always come before
Event B (timestamp 2:05)
```

## Advanced Patterns (Optional)

### Snapshots
Cache aggregate state at certain versions to avoid replaying all events.

```
Snapshot at version 100: Cached state
New event at version 101: Delta from snapshot
New event at version 102: Delta from snapshot
```

### CQRS (Command Query Responsibility Segregation)
```
Commands (writes) → Event Store → Events
                                    ↓
                            Event Subscribers
                                    ↓
                         Read Model (denormalized)
                                    ↓
                              Fast Queries
```

### Event Projections
Create specialized views from events.

```
Events → ProjectionService → ReadModel (optimized for queries)

Example:
Events → Customer Revenue Projection
       → Inventory Projection
       → Payment Analytics Projection
```

## Testing Event Sourcing

```java
@Test
void testEventOrdering() {
    Order order = startOrderSaga(event);
    List<EventStore> history = eventSourcingService.getEventHistory(order.getId());
    
    // Verify event sequence
    assertEquals(4, history.size());
    assertEquals("ORDER_CREATED", history.get(0).getEventType());
    assertEquals("PAYMENT_COMPLETED", history.get(1).getEventType());
    assertEquals("INVENTORY_RESERVED", history.get(2).getEventType());
    assertEquals("ORDER_CONFIRMED", history.get(3).getEventType());
}
```

## Database Schema

```sql
CREATE TABLE event_store (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_data LONGTEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    aggregate_version BIGINT NOT NULL,
    INDEX idx_order_version (order_id, aggregate_version)
);
```

## Common Pitfalls

### 1. ❌ Modifying Events
Events are immutable. Create new compensating events instead.

### 2. ❌ Losing Event Ordering
Always use timestamps or sequence numbers to maintain causality.

### 3. ❌ Not Handling Duplicates
Events might be published twice (network issues). Handle idempotently.

### 4. ❌ Event Store Becomes Event Log
The Event Store is a log, not a queue. Don't treat it as message queue.

## Summary

Event sourcing transforms your application from "what is the state?" to "how did we get here?" This is invaluable for:
- Distributed systems (like your Saga pattern)
- Audit-critical applications
- Debugging production issues
- Building scalable systems with CQRS

Your Saga Pattern + Event Sourcing combo is production-grade distributed transaction management! 🚀
