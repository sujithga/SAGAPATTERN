# Saga Pattern Implementation in Spring Boot

This project demonstrates the **Saga Pattern**, a distributed transaction pattern used in microservices architecture to maintain data consistency across multiple services.

## What is the Saga Pattern?

The Saga Pattern is a way to manage data consistency across multiple services in a distributed transaction scenario. Instead of using traditional ACID transactions spanning multiple databases, a saga is a sequence of local transactions coordinated across services.

### Key Concepts:

1. **Distributed Transaction**: A transaction that spans multiple services
2. **Local Transaction**: Each service performs a local transaction with its own database
3. **Compensation Transaction**: If any step fails, previous steps are rolled back using compensation transactions
4. **Orchestration**: A central orchestrator coordinates the saga steps (implemented in this project)
5. **Choreography**: Alternative approach where services communicate via events (not implemented here)

## Two Approaches to Saga Pattern:

### 1. **Orchestration** (Implemented in this project)
- A central orchestrator (OrderSagaOrchestrator) controls the saga workflow
- The orchestrator tells each service what to do
- Better for complex workflows with many steps
- Easier to understand and debug
- Single point of control

### 2. **Choreography**
- Services publish events when they complete a step
- Other services listen to events and react
- More loosely coupled
- Harder to understand the overall workflow
- Better for systems with many independent services

## Use Case: Order Processing Saga

This project implements an order processing saga with three main steps:

```
Order Created
    ↓
Step 1: Process Payment
    ↓ (success)
Step 2: Reserve Inventory
    ↓ (success)
Step 3: Confirm Order
    ↓
Order Complete

If any step fails:
    ↓
Compensation 1: Refund Payment
    ↓
Compensation 2: Release Inventory
    ↓
Order Cancelled
```

## Project Structure

```
src/main/java/com/example/saga/
├── SagaPatternApplication.java       # Spring Boot main class
├── entity/
│   └── Order.java                    # Order entity with status
├── event/
│   └── SagaEvent.java               # Event classes for saga steps
├── service/
│   ├── PaymentService.java          # Handles payment processing
│   └── InventoryService.java        # Handles inventory management
├── orchestrator/
│   └── OrderSagaOrchestrator.java   # Core saga orchestrator
├── repository/
│   └── OrderRepository.java         # Data access layer
├── controller/
│   └── OrderController.java         # REST API endpoints
└── dto/
    └── CreateOrderRequest.java      # Request DTO
```

## Running the Application

### Prerequisites
- Java 17+
- Maven 3.6+

### Build and Run

```bash
# Navigate to project directory
cd /Users/sujithacharya/SujithMyDirectory/RandD/SagaPattern

# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## API Endpoints

### 1. Create Order (Start Saga)
```bash
POST /api/orders
Content-Type: application/json

{
  "customerId": 1,
  "productId": 1,
  "quantity": 5,
  "amount": 100.00
}
```

**Successful Response (201 Created):**
```json
{
  "id": 1,
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "customerId": 1,
  "productId": 1,
  "quantity": 5,
  "amount": 100.00,
  "status": "ORDER_CONFIRMED"
}
```

**Error Response (400 Bad Request):**
```json
{
  "error": "Order saga failed and rolled back: Insufficient inventory..."
}
```

### 2. Get All Orders
```bash
GET /api/orders
```

### 3. Get Specific Order
```bash
GET /api/orders/{orderId}
```

### 4. Get Inventory Stock
```bash
GET /api/orders/inventory/stock
```

## How the Saga Works

### Success Scenario:
1. **Order Created**: New order with PENDING status
2. **Payment Processing**: PaymentService processes payment
   - If successful → Order status changes to PAYMENT_COMPLETED
   - If fails → Compensation starts
3. **Inventory Reservation**: InventoryService reserves inventory
   - If successful → Order status changes to INVENTORY_RESERVED
   - If fails → Compensation starts
4. **Order Confirmation**: Final confirmation
   - Order status changes to ORDER_CONFIRMED

### Failure Scenario (with Compensation):
If any step fails, the saga automatically triggers compensation:
1. **Payment Refund**: If payment was completed, it's refunded
2. **Inventory Release**: If inventory was reserved, it's released
3. **Order Cancellation**: Order status becomes CANCELLED

## Key Code Points

### OrderSagaOrchestrator.java
The heart of the saga pattern. Key methods:
- `startOrderSaga()`: Orchestrates the entire saga workflow
- `compensateOrderSaga()`: Executes compensation transactions on failure

### Compensation Logic
```java
private void compensateOrderSaga(Order order, OrderCreatedEvent event) {
    // Compensation 1: Refund Payment
    if (order.getStatus() == Order.OrderStatus.PAYMENT_COMPLETED ||
            order.getStatus() == Order.OrderStatus.INVENTORY_RESERVED) {
        paymentService.refundPayment(paymentEvent);
    }

    // Compensation 2: Release Inventory
    if (order.getStatus() == Order.OrderStatus.INVENTORY_RESERVED) {
        inventoryService.releaseInventory(inventoryEvent);
    }
}
```

## Testing Scenarios

### Test 1: Successful Order
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "productId": 1,
    "quantity": 5,
    "amount": 100.00
  }'
```

### Test 2: Insufficient Inventory (Triggers Compensation)
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "productId": 1,
    "quantity": 500,
    "amount": 10000.00
  }'
```

### Test 3: Invalid Amount (Triggers Compensation)
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "productId": 1,
    "quantity": 5,
    "amount": -100.00
  }'
```

## Initial Inventory

The application starts with the following inventory:
- Product ID 1: 100 units
- Product ID 2: 50 units
- Product ID 3: 75 units

## Database

The application uses H2 in-memory database for easy testing. You can access the H2 console at:
```
http://localhost:8080/h2-console
```

Database credentials:
- URL: jdbc:h2:mem:testdb
- Username: sa
- Password: (empty)

## Important Concepts

### 1. Idempotency
In a real distributed system, compensation transactions might be called multiple times. Services should be idempotent to handle duplicate calls safely.

### 2. Timeouts
The saga orchestrator should have timeout mechanisms. If a step takes too long, compensation should be triggered.

### 3. Retry Logic
If a service is temporarily unavailable, the saga should retry before triggering compensation.

### 4. Event Sourcing
In production, all saga events should be logged for audit trails and recovery purposes.

### 5. Deadletter Queue
Failed sagas that couldn't be compensated should be stored in a dead-letter queue for manual intervention.

## Advantages of Saga Pattern

✅ Maintains data consistency across services  
✅ Avoids distributed locks  
✅ Each service has its own database  
✅ Scales well in microservices  
✅ Handles long-running transactions  

## Disadvantages of Saga Pattern

❌ More complex than traditional transactions  
❌ Requires compensation logic  
❌ Eventual consistency (not immediate consistency)  
❌ Harder to debug  
❌ Requires event coordination mechanism  

## Real-World Applications

- **E-commerce**: Order processing with payment and inventory
- **Hotel Booking**: Reservations, payments, and confirmations
- **Travel Booking**: Flight, hotel, car rental coordination
- **Banking**: Fund transfers between accounts
- **Ride Sharing**: Matching, payment, and rating

## Further Reading

1. [Pattern: Saga - Chris Richardson](https://microservices.io/patterns/data/saga.html)
2. [Saga Pattern in Microservices - DDD](https://www.nginx.com/blog/microservices-architecture-how-to-manage-consistency-between-services/)
3. [Choreography vs Orchestration](https://www.nginx.com/blog/event-driven-data-management-for-microservices/)

## Author Notes

This implementation demonstrates:
- Clean separation of concerns
- Event-driven architecture
- Compensation-based transactions
- Orchestration pattern
- Spring Boot best practices

Feel free to extend this project with:
- Event sourcing
- Message queuing (Kafka, RabbitMQ)
- Async processing
- Circuit breakers
- Retry mechanisms
- Monitoring and alerting
