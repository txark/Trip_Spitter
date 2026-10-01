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

    // เข้าทริปเดิมซ้ำ = อัปเดตเวลาล่าสุดของแถวเดิม (เดิมเพิ่มแถวใหม่ทุกครั้ง ทำให้ "ทริปล่าสุด" ซ้ำกันจนเหลือทริปเดียว)
    @Transactional
    public void recordTripView(Long userId, Long tripId) {
        if (userId == null || tripId == null) {
            return;
        }
        List<UserTripHistory> rows = historyRepo.findByUserIdAndTripIdOrderByViewedAtDesc(userId, tripId);
        UserTripHistory history = rows.isEmpty() ? new UserTripHistory() : rows.get(0);
        // ลบแถวซ้ำที่ค้างจากเวอร์ชันเก่า
        if (rows.size() > 1) {
            historyRepo.deleteAll(rows.subList(1, rows.size()));
        }
        history.setUserId(userId);
        history.setTripId(tripId);
        history.setViewedAt(LocalDateTime.now());
        historyRepo.save(history);
    }
}