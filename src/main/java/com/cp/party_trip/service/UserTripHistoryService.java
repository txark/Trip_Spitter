package com.cp.party_trip.service;

import com.cp.party_trip.model.UserTripHistory;
import com.cp.party_trip.repository.UserTripHistoryRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserTripHistoryService {
    private final UserTripHistoryRepo historyRepo;

    public UserTripHistoryService(UserTripHistoryRepo historyRepo) {
        this.historyRepo = historyRepo;
    }

    public List<UserTripHistory> getRecentTrips(Long userId) {
        return historyRepo.findTop5ByUserIdOrderByViewedAtDesc(userId);
    }

    @Transactional
    public void recordTripView(Long userId, Long tripId) {
        UserTripHistory history = new UserTripHistory();
        history.setUserId(userId);
        history.setTripId(tripId);
        history.setViewedAt(LocalDateTime.now());
        historyRepo.save(history);
    }
}