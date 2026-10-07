package com.cp.party_trip.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.List;

// เพิ่มของหลายชิ้นในครั้งเดียว: ทุกชิ้นใช้หมวดและผู้รับผิดชอบชุดเดียวกัน
// แต่ละชิ้นมีจำนวน/หน่วย/โน้ตของตัวเอง
public class ChecklistBulkRequest {
    @NotNull(message = "ไม่ได้ระบุทริป")
    private Long tripId;
    private String category;
    private List<Long> assigneeIds;
    @NotEmpty(message = "กรุณาเพิ่มอย่างน้อย 1 ชิ้น")
    @Size(max = 50, message = "เพิ่มได้ครั้งละไม่เกิน 50 ชิ้น")
    @Valid
    private List<NewItem> items;

    public static class NewItem {
        @Size(max = 100, message = "ชื่อสิ่งของยาวได้ไม่เกิน 100 ตัวอักษร")
        private String itemName;
        @DecimalMin(value = "0", message = "จำนวนต้องไม่ติดลบ")
        private BigDecimal quantity;
        @Size(max = 20, message = "หน่วยยาวได้ไม่เกิน 20 ตัวอักษร")
        private String unit;
        @Size(max = 255, message = "โน้ตยาวได้ไม่เกิน 255 ตัวอักษร")
        private String notes;

        public String getItemName() {
            return itemName;
        }

        public void setItemName(String itemName) {
            this.itemName = itemName;
        }

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
    }

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<Long> getAssigneeIds() {
        return assigneeIds;
    }

    public void setAssigneeIds(List<Long> assigneeIds) {
        this.assigneeIds = assigneeIds;
    }

    public List<NewItem> getItems() {
        return items;
    }

    public void setItems(List<NewItem> items) {
        this.items = items;
    }
}
