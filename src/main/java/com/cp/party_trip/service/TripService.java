package com.cp.party_trip.service;

import com.cp.party_trip.model.Trip;
import com.cp.party_trip.model.TripMember;
import java.math.BigDecimal;
import java.util.List;

// ทริป: สร้าง เข้าร่วม และตั้งค่าทริป (งบ สกุลเงิน วันเดินทาง เขตเวลา)
// ตัวจริงอยู่ที่ service/impl/TripServiceImpl (Controller ขึ้นกับ interface นี้ ไม่ใช่คลาสจริง)
public interface TripService {
    Trip createTrip(Trip trip, String creatorName);

    TripMember joinTrip(String inviteCode, String userName);

    // ตั้งงบต่อคน: ไม่ส่ง / 0 = ยกเลิกงบ
    Trip updateBudget(Long tripId, Long memberId, BigDecimal amount);

    // ตั้งสกุลเงินท้องถิ่นของทริป: ไม่ส่ง / THB = ใช้บาทอย่างเดียว
    Trip updateCurrency(Long tripId, Long memberId, String currency, BigDecimal rate);

    // แก้วันเริ่ม/วันสิ้นสุดของทริป: สมาชิกคนไหนก็แก้ได้ (บิล/แพลนเดิมไม่ถูกลบ แม้อยู่นอกช่วงใหม่)
    Trip updateDates(Long tripId, Long memberId, String start, String end);

    // เปลี่ยนเขตเวลาของทริป: สมาชิกคนไหนก็เปลี่ยนได้ (เหมือนแก้แพลน)
    Trip updateTimeZone(Long tripId, Long memberId, String timeZone);

    List<TripMember> getMembers(Long tripId);

    // สมาชิกคนนี้ต้องอยู่ในทริปนี้ (ไม่พบ = 404)
    TripMember getMember(Long tripId, Long memberId);

    Trip getTripById(Long id);
}
