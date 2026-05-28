package com.example.saga.controller;

import com.example.saga.entity.EventStore;
import com.example.saga.service.EventSourcingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventHistoryController {
    private final EventSourcingService eventSourcingService;

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<EventStore>> getOrderEventHistory(@PathVariable Long orderId) {
        List<EventStore> events = eventSourcingService.getEventHistory(orderId);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/all")
    public ResponseEntity<List<EventStore>> getAllEvents() {
        List<EventStore> events = eventSourcingService.getAllEvents();
        return ResponseEntity.ok(events);
    }
}
