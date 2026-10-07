package com.cp.party_trip.event;

import java.math.BigDecimal;

// เหตุการณ์: มีการเพิ่มบิลในทริป (ส่งจาก ExpenseServiceImpl)
public record ExpenseAddedEvent(Long tripId, Long memberId, String memberName, String title, BigDecimal amount) {
}
