package com.cp.party_trip.repository;

import com.cp.party_trip.model.UserTripHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserTripHistoryRepo extends JpaRepository<UserTripHistory, Long> {
    List<UserTripHistory> findTop5ByUserIdOrderByViewedAtDesc(Long userId);
}