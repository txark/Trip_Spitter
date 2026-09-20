package com.cp.party_trip.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "settlements")
public class Settlement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private TripMember sender; // ผู้โอนเงิน (ลูกหนี้)

    @ManyToOne
    @JoinColumn(name = "receiver_id")
    private TripMember receiver; // ผู้รับเงิน (เจ้าหนี้)

    private BigDecimal amount;

    @Column(name = "settled_at", updatable = false)
    private LocalDateTime settledAt = LocalDateTime.now();

    // Getters & Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Trip getTrip() {
        return trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }

    public TripMember getSender() {
        return sender;
    }

    public void setSender(TripMember sender) {
        this.sender = sender;
    }

    public TripMember getReceiver() {
        return receiver;
    }

    public void setReceiver(TripMember receiver) {
        this.receiver = receiver;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getSettledAt() {
        return settledAt;
    }

    public void setSettledAt(LocalDateTime settledAt) {
        this.settledAt = settledAt;
    }
}