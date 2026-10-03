package com.cp.party_trip.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// เพื่อนโอนคืนเป็นยอดรวม (ไม่ระบุรายการ) แล้วระบบหักเคลียร์รายการให้ตามลำดับ
@Entity
@Table(name = "repayments")
public class Repayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long tripId;
    private Long fromMemberId; // คนที่โอนคืน
    private Long toMemberId; // คนที่สำรองจ่ายไว้ (ผู้รับเงิน)

    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "repayment_items", joinColumns = @JoinColumn(name = "repayment_id"))
    @OrderColumn(name = "position")
    private List<RepaymentItem> items = new ArrayList<>();

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

    public Long getFromMemberId() {
        return fromMemberId;
    }

    public void setFromMemberId(Long fromMemberId) {
        this.fromMemberId = fromMemberId;
    }

    public Long getToMemberId() {
        return toMemberId;
    }

    public void setToMemberId(Long toMemberId) {
        this.toMemberId = toMemberId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<RepaymentItem> getItems() {
        return items;
    }

    public void setItems(List<RepaymentItem> items) {
        this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
    }
}
