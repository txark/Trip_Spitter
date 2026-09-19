package com.cp.party_trip.repository;

import com.cp.party_trip.model.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ActivityRepo extends JpaRepository<Activity, Long> {
    List<Activity> findByTripId(Long tripId);
}