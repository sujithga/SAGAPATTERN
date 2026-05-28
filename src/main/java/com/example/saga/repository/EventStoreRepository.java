package com.example.saga.repository;

import com.example.saga.entity.EventStore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventStoreRepository extends JpaRepository<EventStore, Long> {
    List<EventStore> findByOrderIdOrderByAggregateVersionAsc(Long orderId);
    List<EventStore> findAllByOrderByIdAsc();
}
