package com.cp.party_trip.dto;

import java.math.BigDecimal;

// แก้รายละเอียดของชิ้นหนึ่ง: จำนวน/หน่วย/โน้ต (ค่าว่าง = ล้าง)
public class ChecklistDetailsRequest {
    private BigDecimal quantity;
    private String unit;
    private String notes;
    private Long memberId;

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }
}
