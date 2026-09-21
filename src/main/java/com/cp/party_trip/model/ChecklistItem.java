package com.cp.party_trip.model;

import jakarta.persistence.*;

@Entity
@Table(name = "checklist_items")
public class ChecklistItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_id")
    private Long tripId;

    private String category; // เช่น อุปกรณ์ปาร์ตี้, ยา, ของใช้ส่วนตัว

    @Column(name = "item_name")
    private String itemName; // ชื่อสิ่งของ

    @Column(name = "assigned_to_member_id")
    private Long assignedToMemberId; // ผู้รับผิดชอบหยิบของ

    @Column(name = "is_checked")
    private boolean isChecked = false; // สถานะการเตรียมของ

    @Column(name = "updated_by_member_id")
    private Long updatedByMemberId; // สมาชิกคนที่อัปเดตโน้ตล่าสุด

    private String notes; // โน้ตย่อยหรือความคิดเห็นเพิ่มเติม (เช่น ยี่ห้อ, ขนาด, ร้านที่ต้องซื้อ)

    // Getters & Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Long getAssignedToMemberId() {
        return assignedToMemberId;
    }

    public void setAssignedToMemberId(Long assignedToMemberId) {
        this.assignedToMemberId = assignedToMemberId;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getUpdatedByMemberId() {
        return updatedByMemberId;
    }

    public void setUpdatedByMemberId(Long updatedByMemberId) {
        this.updatedByMemberId = updatedByMemberId;
    }
}