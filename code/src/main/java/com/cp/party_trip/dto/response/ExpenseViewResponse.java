package com.cp.party_trip.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// บิลในรายการบิลของทริป (ส่ง splits ซ้ำเป็น expenseSplits ด้วย ให้หน้าเว็บเดิมใช้ได้)
public record ExpenseViewResponse(
        Long id,
        String title,
        BigDecimal totalAmount,
        String category,
        String splitType,
        LocalDateTime expenseDate,
        Long activityId,
        Long recordedById,
        int revision,
        String currency,
        BigDecimal originalAmount,
        BigDecimal exchangeRate,
        MemberRefResponse user,
        List<Split> splits,
        List<Split> expenseSplits) {

    public record Split(
            Long id,
            BigDecimal amountOwed,
            @JsonProperty("isPaid") boolean isPaid,
            BigDecimal paidAmount,
            MemberRefResponse tripMember) {
    }
}
