package com.cp.party_trip.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// การรับเงินก้อน + บิลที่ถูกหัก
public record RepaymentResponse(
        Long id,
        Long tripId,
        Long fromMemberId,
        Long toMemberId,
        BigDecimal amount,
        LocalDateTime createdAt,
        List<Item> items) {

    public record Item(Long expenseId, Long splitId, String title, BigDecimal amount) {
    }
}
