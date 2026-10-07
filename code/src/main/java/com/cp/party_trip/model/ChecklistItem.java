package com.cp.party_trip.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "checklist_items", indexes = @Index(name = "idx_checklist_items_trip", columnList = "trip_id"))
public class ChecklistItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_id")
    private Long tripId;

    private String category; // เช่น GEAR, CLOTHES, MEDICINE (รหัสหมวดจากหน้าเว็บ)

    @Column(name = "item_name")
    private String itemName; // ชื่อสิ่งของ

    // จำนวนและหน่วย เช่น 2 ขวด, 1.5 กก. (ไม่ระบุได้)
    @Column(precision = 10, scale = 2)
    private BigDecimal quantity;

    private String unit;

    // ผู้รับผิดชอบคนแรก (คอลัมน์เดิม เก็บไว้ให้ข้อมูลเก่ายังอ่านได้)
    @Column(name = "assigned_to_member_id")
    private Long assignedToMemberId;

    // ผู้รับผิดชอบทั้งหมด (มีได้หลายคน) Many-to-Many กับ TripMember ผ่านตารางกลาง checklist_item_assignees
    // ไม่ใช้ cascade: สมาชิกเป็นของทริป ลบรายการสิ่งของแล้วต้องลบแค่แถวในตารางกลาง
    // EAGER: ใช้แสดงอวตารผู้รับผิดชอบทุกครั้งที่อ่านรายการ (ชุดเล็ก)
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "checklist_item_assignees",
            joinColumns = @JoinColumn(name = "item_id"),
            inverseJoinColumns = @JoinColumn(name = "member_id"))
    @OrderColumn(name = "position")
    private List<TripMember> assignees = new ArrayList<>();

    @Column(name = "is_checked")
    private boolean isChecked = false; // สถานะการเตรียมของ

    @Column(name = "updated_by_member_id")
    private Long updatedByMemberId; // สมาชิกคนที่อัปเดตล่าสุด

    private String notes; // โน้ตของชิ้นนี้ (เช่น ยี่ห้อ, ขนาด, ร้านที่ต้องซื้อ)

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

    public Long getAssignedToMemberId() {
        return assignedToMemberId;
    }

    // คอลัมน์เดิม (ผู้รับผิดชอบคนแรก) ใช้กับข้อมูลเก่าเท่านั้น
    public void setAssignedToMemberId(Long assignedToMemberId) {
        this.assignedToMemberId = assignedToMemberId;
    }

    public List<TripMember> getAssignees() {
        return assignees;
    }

    public void setAssignees(List<TripMember> members) {
        assignees.clear();
        if (members != null) {
            assignees.addAll(members);
        }
        assignedToMemberId = assignees.isEmpty() ? null : assignees.get(0).getId();
    }

    // รหัสผู้รับผิดชอบ (ข้อมูลเก่าที่มีแค่ assigned_to_member_id ให้ถือว่าเป็นรายชื่อ 1 คน)
    public List<Long> getAssigneeIds() {
        if (assignees.isEmpty() && assignedToMemberId != null) {
            return List.of(assignedToMemberId);
        }
        return assignees.stream().map(TripMember::getId).toList();
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
