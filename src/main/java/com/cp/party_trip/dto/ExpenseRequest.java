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
    private Long activityId; // บันทึกจากรายการในแพลน (ไม่บังคับ)
    private java.time.LocalDate expenseDate; // วันที่จ่าย (ไม่ส่ง = วันนี้ / วันเดิมของบิล)
    private java.math.BigDecimal originalAmount; // เงินต่างประเทศ: ยอดตามใบเสร็จ
    private java.math.BigDecimal exchangeRate; // 1 หน่วย = กี่บาท
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

    public java.math.BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(java.math.BigDecimal originalAmount) {
        this.originalAmount = originalAmount;
    }

    public java.math.BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(java.math.BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public java.time.LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(java.time.LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
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
