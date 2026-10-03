package com.cp.party_trip.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

// ยอดที่หักไปเคลียร์รายการหนึ่ง (ไว้แสดง และไว้คืนยอดตอนยกเลิก)
@Embeddable
public class RepaymentItem {
    private Long expenseId;
    private Long splitId;

    @Column(length = 150)
    private String title;

    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    public Long getExpenseId() {
        return expenseId;
    }

    public void setExpenseId(Long expenseId) {
        this.expenseId = expenseId;
    }

    public Long getSplitId() {
        return splitId;
    }

    public void setSplitId(Long splitId) {
        this.splitId = splitId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
