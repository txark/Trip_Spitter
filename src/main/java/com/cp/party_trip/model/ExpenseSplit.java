package com.cp.party_trip.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "expense_splits")
public class ExpenseSplit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "expense_id")
    @JsonBackReference
    private Expense expense;

    @ManyToOne
    @JoinColumn(name = "trip_member_id")
    // @JsonBackReference
    private TripMember tripMember;

    private BigDecimal amountOwed; // Amount for this split
    private BigDecimal percentage; // Percentage of the total expense this member is responsible for
    // ต้องมี default false ไม่งั้น ddl-auto=update เพิ่มคอลัมน์ NOT NULL ลงตารางที่มีข้อมูลอยู่แล้วไม่ได้
    @Column(name = "is_paid", nullable = false, columnDefinition = "boolean not null default false")
    private boolean isPaid = false;

    // จ่ายคืนแล้วเท่าไร (รับเงินเป็นยอดรวมแล้วหักมาบางส่วนได้) null = ยังไม่เคยจ่าย
    @Column(name = "paid_amount", precision = 10, scale = 2)
    private BigDecimal paidAmount;

    // ส่วนที่จ่ายคืนแล้วจริง: กดยืนยันครบ = ทั้งก้อน, ไม่งั้น = ยอดที่หักมา (ไม่ใช่ getter กัน JSON วนลูป)
    public BigDecimal paidSoFar() {
        BigDecimal owed = amountOwed == null ? BigDecimal.ZERO : amountOwed;
        if (isPaid) {
            return owed;
        }
        BigDecimal paid = paidAmount == null ? BigDecimal.ZERO : paidAmount;
        return paid.min(owed);
    }

    public BigDecimal remainingAmount() {
        BigDecimal owed = amountOwed == null ? BigDecimal.ZERO : amountOwed;
        return owed.subtract(paidSoFar());
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public Long getTripMemberId() {
        return tripMember != null ? tripMember.getId() : null;
    }

    // Getters & Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Expense getExpense() {
        return expense;
    }

    public void setExpense(Expense expense) {
        this.expense = expense;
    }

    public TripMember getTripMember() {
        return tripMember;
    }

    public void setTripMember(TripMember tripMember) {
        this.tripMember = tripMember;
    }

    public BigDecimal getAmountOwed() {
        return amountOwed;
    }

    public void setAmountOwed(BigDecimal amountOwed) {
        this.amountOwed = amountOwed;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }

    public boolean isPaid() {
        return isPaid;
    }

    public void setPaid(boolean paid) {
        isPaid = paid;
    }
}
