package com.cp.party_trip.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "polls", indexes = @Index(name = "idx_polls_trip", columnList = "trip_id"))
public class Poll {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long tripId;
    private String question;
    private String status = "ACTIVE"; // ACTIVE หรือ CLOSED

    // สมาชิกที่สร้างโหวต (TripMember.id) — มีสิทธิ์ปิดโหวต
    private Long createdByMemberId;
    private LocalDateTime createdAt = LocalDateTime.now();

    // เวลาปิดรับโหวตอัตโนมัติ (null = เปิดจนกว่าคนสร้างจะกดปิด)
    private LocalDateTime closesAt;

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

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCreatedByMemberId() {
        return createdByMemberId;
    }

    public void setCreatedByMemberId(Long createdByMemberId) {
        this.createdByMemberId = createdByMemberId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getClosesAt() {
        return closesAt;
    }

    public void setClosesAt(LocalDateTime closesAt) {
        this.closesAt = closesAt;
    }
}
