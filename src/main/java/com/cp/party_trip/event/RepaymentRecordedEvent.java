package com.cp.party_trip.event;

import java.math.BigDecimal;

// เหตุการณ์: มีการรับเงินคืนในทริป (ส่งจาก RepaymentServiceImpl)
public record RepaymentRecordedEvent(Long tripId, Long receiverId, String receiverName, String senderName,
        BigDecimal amount) {
}
