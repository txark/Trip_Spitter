package com.cp.party_trip.repository;

import com.cp.party_trip.model.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SettlementRepo extends JpaRepository<Settlement, Long> {
    List<Settlement> findByTripId(Long tripId);
}