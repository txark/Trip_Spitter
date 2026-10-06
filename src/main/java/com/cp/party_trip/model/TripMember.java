package com.cp.party_trip.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
// คนเดียวกันอยู่ในทริปเดียวกันได้แถวเดียว (กดเข้าร่วมซ้ำพร้อมกันเคยได้สมาชิกซ้ำ)
@Table(name = "trip_members", uniqueConstraints = @UniqueConstraint(name = "uk_trip_members_trip_guest",
        columnNames = { "trip_id", "guest_name" }))
public class TripMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "trip_id", nullable = false)
    @JsonIgnore // ป้องกัน Infinite Recursion เวลา Return JSON
    private Trip trip;

    @Column(name = "guest_name", nullable = false)
    private String guestName;

    @Column(nullable = false)
    private String role = "MEMBER"; // ADMIN หรือ MEMBER

    @Column(name = "joined_at")
    private LocalDateTime joinedAt = LocalDateTime.now();

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

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    // Alias Methods
    public String getUserName() {
        return guestName;
    }

    public void setUserName(String userName) {
        this.guestName = userName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }
}