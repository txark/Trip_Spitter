package com.cp.party_trip.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// สรุปหนี้ของสมาชิกคนหนึ่ง: ต้องจ่ายใคร, บิลที่สำรองจ่าย, การรับเงินก้อน
public record MemberDebtSummaryResponse(
        List<DebtGroup> myDebts,
        List<PaidBill> myPaidBills,
        List<RepaymentView> repayments) {

    // หนี้ที่ต้องจ่ายให้เจ้าหนี้คนหนึ่ง (รวมทุกบิล)
    public record DebtGroup(Long creditorId, String creditorName, BigDecimal totalAmount, List<DebtItem> items) {
    }

    // amount = ยอดที่ยังค้าง, owed = ส่วนของเราในบิล, paid = จ่ายไปแล้วบางส่วน
    public record DebtItem(String title, BigDecimal amount, BigDecimal owed, BigDecimal paid) {
    }

    // บิลที่เราสำรองจ่าย + ส่วนของเพื่อนแต่ละคน
    public record PaidBill(Long expenseId, String title, BigDecimal totalAmount, List<PaidSplit> splits) {
    }

    public record PaidSplit(
            Long expenseId,
            Long memberId,
            String memberName,
            BigDecimal amountOwed,
            @JsonProperty("isPaid") boolean isPaid,
            BigDecimal paidAmount) {
    }

    // การรับเงินก้อนที่เกี่ยวกับเรา (เป็นคนรับหรือคนโอน)
    public record RepaymentView(
            Long id,
            Long fromMemberId,
            String fromName,
            Long toMemberId,
            String toName,
            BigDecimal amount,
            LocalDateTime createdAt,
            List<Item> items) {

        public record Item(Long expenseId, String title, BigDecimal amount) {
        }
    }
}
