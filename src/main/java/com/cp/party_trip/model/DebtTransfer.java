package com.cp.party_trip.model;

import java.math.BigDecimal;

public class DebtTransfer {
    private BigDecimal amount;
    private TripMember from;
    private TripMember to;

    public DebtTransfer(BigDecimal amount, TripMember from, TripMember to) {
        this.amount = amount;
        this.from = from;
        this.to = to;
    }

    // เมธอดสำรองป้องกันการเรียกชื่อฟิลด์สลับกัน
    public TripMember getSender() {
        return from;
    }

    public TripMember getReceiver() {
        return to;
    }

    // Getters and Setters
    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TripMember getFrom() {
        return from;
    }

    public void setFrom(TripMember from) {
        this.from = from;
    }

    public TripMember getTo() {
        return to;
    }

    public void setTo(TripMember to) {
        this.to = to;
    }
}