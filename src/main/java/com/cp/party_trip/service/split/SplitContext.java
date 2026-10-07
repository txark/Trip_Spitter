package com.cp.party_trip.service.split;

import com.cp.party_trip.dto.request.ExpenseRequest;

import java.math.BigDecimal;
import java.util.List;

// ข้อมูลที่ทุกวิธีหารได้รับ: ยอดบิล (ปัด 2 ตำแหน่งแล้ว), ผู้จ่าย, รายชื่อผู้ร่วมหาร (EQUAL), ยอดรายคน (CUSTOM)
public record SplitContext(BigDecimal totalAmount, Long payerId, List<Long> participantIds,
        List<ExpenseRequest.SplitAmount> customAmounts) {
}
