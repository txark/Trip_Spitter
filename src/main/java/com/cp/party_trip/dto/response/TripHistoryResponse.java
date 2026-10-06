package com.cp.party_trip.dto.response;

import java.time.LocalDateTime;

// ทริปที่ผู้ใช้เปิดดูล่าสุด
public record TripHistoryResponse(Long id, Long userId, Long tripId, LocalDateTime viewedAt) {
}
