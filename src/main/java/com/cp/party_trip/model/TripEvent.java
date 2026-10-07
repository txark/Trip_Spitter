package com.cp.party_trip.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// บันทึกความเคลื่อนไหวของทริป (เพิ่มบิล, รับเงินคืน) หลายรายการต่อหนึ่งทริป (One-to-Many)
@Entity
@Table(name = "trip_events", indexes = @Index(name = "idx_trip_events_trip_created", columnList = "trip_id, created_at"))
public class TripEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // อ้างด้วย id เฉยๆ ไม่ต้องโหลดทริปทั้งก้อนเพื่อบันทึกประวัติ
    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    @Column(nullable = false)
    private String message;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
