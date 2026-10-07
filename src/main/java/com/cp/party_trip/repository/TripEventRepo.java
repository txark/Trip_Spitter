package com.cp.party_trip.repository;

import com.cp.party_trip.model.TripEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripEventRepo extends JpaRepository<TripEvent, Long> {
    List<TripEvent> findTop20ByTripIdOrderByCreatedAtDescIdDesc(Long tripId);
}
