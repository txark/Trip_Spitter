package com.cp.party_trip.dto;

import java.math.BigDecimal;
import java.util.List;

// เพิ่มของหลายชิ้นในครั้งเดียว: ทุกชิ้นใช้หมวดและผู้รับผิดชอบชุดเดียวกัน
// แต่ละชิ้นมีจำนวน/หน่วย/โน้ตของตัวเอง
public class ChecklistBulkRequest {
    private Long tripId;
    private String category;
    private List<Long> assigneeIds;
    private List<NewItem> items;

    public static class NewItem {
        private String itemName;
        private BigDecimal quantity;
        private String unit;
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
