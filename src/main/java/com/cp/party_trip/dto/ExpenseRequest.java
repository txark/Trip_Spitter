package com.cp.party_trip.dto;

import java.math.BigDecimal;
import java.util.List;

// ข้อมูลที่หน้าเว็บส่งมาตอนบันทึกบิล
// splitType = EQUAL  -> หารเท่ากันตาม participantIds (ไม่ต้องส่ง splits)
// splitType = CUSTOM -> ใช้ยอดของแต่ละคนจาก splits ผลรวมต้องเท่ากับ totalAmount
public class ExpenseRequest {

    private String title;
    private BigDecimal totalAmount;
    private String currency;
    private String splitType;
    private String category;
    private List<SplitAmount> splits;

    public static class SplitAmount {
        private Long memberId;
        private BigDecimal amount;

        public Long getMemberId() {
            return memberId;
        }

        public void setMemberId(Long memberId) {
            this.memberId = memberId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }

    // Getters & Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getSplitType() {
        return splitType;
    }

    public void setSplitType(String splitType) {
        this.splitType = splitType;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<SplitAmount> getSplits() {
        return splits;
    }

    public void setSplits(List<SplitAmount> splits) {
        this.splits = splits;
    }
}
