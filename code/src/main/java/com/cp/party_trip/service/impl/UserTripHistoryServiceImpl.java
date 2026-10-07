package com.cp.party_trip.service.impl;

import com.cp.party_trip.service.UserTripHistoryService;
import com.cp.party_trip.model.User;
import com.cp.party_trip.model.UserTripHistory;
import com.cp.party_trip.repository.TripMemberRepo;
import com.cp.party_trip.repository.UserTripHistoryRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserTripHistoryServiceImpl implements UserTripHistoryService {
    private final UserTripHistoryRepo historyRepo;
    private final TripMemberRepo tripMemberRepo;

    public UserTripHistoryServiceImpl(UserTripHistoryRepo historyRepo, TripMemberRepo tripMemberRepo) {
        this.historyRepo = historyRepo;
        this.tripMemberRepo = tripMemberRepo;
    }

    // ทริปล่าสุดของผู้ใช้ เฉพาะทริปที่ยังเป็นสมาชิกอยู่ (ทริปอื่นเปิดไปก็ 403)
    @Override
    public List<UserTripHistory> getRecentTrips(User user) {
        return historyRepo.findTop5ByUserIdOrderByViewedAtDesc(user.getId()).stream()
                .filter(h -> tripMemberRepo
                        .findFirstByTripIdAndGuestNameOrderByIdAsc(h.getTripId(), user.getUsername()).isPresent())
                .toList();
    }

    // เข้าทริปเดิมซ้ำ = อัปเดตเวลาล่าสุดของแถวเดิม (เดิมเพิ่มแถวใหม่ทุกครั้ง ทำให้ "ทริปล่าสุด" ซ้ำกันจนเหลือทริปเดียว)
    @Override
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