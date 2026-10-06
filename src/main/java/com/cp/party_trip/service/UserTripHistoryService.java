package com.cp.party_trip.service;

import com.cp.party_trip.model.User;
import com.cp.party_trip.model.UserTripHistory;
import java.util.List;

// ประวัติทริปที่ผู้ใช้เปิดดูล่าสุด
// ตัวจริงอยู่ที่ service/impl/UserTripHistoryServiceImpl (Controller ขึ้นกับ interface นี้ ไม่ใช่คลาสจริง)
public interface UserTripHistoryService {
    // ทริปล่าสุดของผู้ใช้ เฉพาะทริปที่ยังเป็นสมาชิกอยู่ (ทริปอื่นเปิดไปก็ 403)
    List<UserTripHistory> getRecentTrips(User user);

    // เข้าทริปเดิมซ้ำ = อัปเดตเวลาล่าสุดของแถวเดิม (เดิมเพิ่มแถวใหม่ทุกครั้ง ทำให้ "ทริปล่าสุด" ซ้ำกันจนเหลือทริปเดียว)
    void recordTripView(Long userId, Long tripId);
}
