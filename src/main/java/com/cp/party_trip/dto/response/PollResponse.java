package com.cp.party_trip.dto.response;

import java.time.LocalDateTime;

// โหวต (ตอนสร้าง/ปิด) ผลโหวตดูที่ PollSummaryResponse
public record PollResponse(
        Long id,
        Long tripId,
        String question,
        String status,
        Long createdByMemberId,
        LocalDateTime createdAt,
        LocalDateTime closesAt) {
}
