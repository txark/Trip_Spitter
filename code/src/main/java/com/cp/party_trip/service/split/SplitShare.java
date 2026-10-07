package com.cp.party_trip.service.split;

import java.math.BigDecimal;

// ส่วนที่สมาชิกหนึ่งคนต้องจ่ายในบิลหนึ่งใบ (ผลลัพธ์ของ SplitStrategy)
public record SplitShare(Long memberId, BigDecimal amount) {
}
