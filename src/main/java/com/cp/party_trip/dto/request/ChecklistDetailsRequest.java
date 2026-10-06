package com.cp.party_trip.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// แก้รายละเอียดของชิ้นหนึ่ง: จำนวน/หน่วย/โน้ต (ค่าว่าง = ล้าง)
public class ChecklistDetailsRequest {
    @DecimalMin(value = "0", message = "จำนวนต้องไม่ติดลบ")
    private BigDecimal quantity;
    @Size(max = 20, message = "หน่วยยาวได้ไม่เกิน 20 ตัวอักษร")
    private String unit;
    @Size(max = 255, message = "โน้ตยาวได้ไม่เกิน 255 ตัวอักษร")
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
