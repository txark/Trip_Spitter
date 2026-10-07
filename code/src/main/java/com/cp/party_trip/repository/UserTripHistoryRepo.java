package com.cp.party_trip.repository;

import com.cp.party_trip.model.UserTripHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserTripHistoryRepo extends JpaRepository<UserTripHistory, Long> {
    List<UserTripHistory> findTop5ByUserIdOrderByViewedAtDesc(Long userId);

    // ค้นหาประวัติเดิมเพื่อป้องกันการบันทึกซ้ำ
    Optional<UserTripHistory> findByUserIdAndTripId(Long userId, Long tripId);

    // ทุกแถวของคู่ user+trip (ข้อมูลเก่าอาจซ้ำหลายแถว) ใหม่สุดก่อน
    List<UserTripHistory> findByUserIdAndTripIdOrderByViewedAtDesc(Long userId, Long tripId);
}