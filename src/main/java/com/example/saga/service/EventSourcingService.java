package com.example.saga.service;

import com.example.saga.entity.EventStore;
import com.example.saga.event.SagaEvent;
import com.example.saga.repository.EventStoreRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventSourcingService {
    private final EventStoreRepository eventStoreRepository;
    private final ObjectMapper objectMapper;

    public void appendEvent(Long orderId, SagaEvent event, Long aggregateVersion) {
        try {
            String eventData = objectMapper.writeValueAsString(event);
            EventStore eventStore = new EventStore(
                    orderId,
                    event.getEventType(),
                    eventData,
                    aggregateVersion
            );
            eventStoreRepository.save(eventStore);
            log.info("Event persisted for Order ID: {} - Type: {} - Version: {}",
                    orderId, event.getEventType(), aggregateVersion);
        } catch (Exception e) {
            log.error("Failed to persist event for order: {}", orderId, e);
            throw new RuntimeException("Event sourcing failed: " + e.getMessage());
        }
    }

    public List<EventStore> getEventHistory(Long orderId) {
        return eventStoreRepository.findByOrderIdOrderByAggregateVersionAsc(orderId);
    }

    public List<EventStore> getAllEvents() {
        return eventStoreRepository.findAllByOrderByIdAsc();
    }
}
