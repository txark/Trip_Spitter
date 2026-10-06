package com.cp.party_trip.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// บิลที่เพิ่งเพิ่ม/แก้ (ส่วนแบ่งของแต่ละคนอยู่ใน expenseSplits)
public record ExpenseResponse(
        Long id,
        String title,
        BigDecimal totalAmount,
        String currency,
        BigDecimal originalAmount,
        BigDecimal exchangeRate,
        String splitType,
        String category,
        LocalDateTime expenseDate,
        Long activityId,
        Long recordedById,
        int revision,
        MemberResponse user,
        List<Split> expenseSplits) {

    // ส่วนที่สมาชิกแต่ละคนต้องจ่ายในบิลนี้
    public record Split(
            Long id,
            Long tripMemberId,
            MemberResponse tripMember,
            BigDecimal amountOwed,
            BigDecimal percentage,
            boolean paid,
            BigDecimal paidAmount) {
    }
}
