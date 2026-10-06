package com.cp.party_trip.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // token ของเครื่องที่ใช้ชื่อนี้ (ส่งมาใน header X-Auth-Token ทุกครั้งที่แก้ข้อมูล) ห้ามหลุดออกไปใน JSON
    @JsonIgnore
    @Column(name = "auth_token", length = 64, unique = true)
    private String authToken;

    // PIN สำหรับเข้าชื่อนี้จากเครื่องอื่น เก็บแบบ hash (salt:hash)
    @JsonIgnore
    @Column(name = "pin_hash", length = 120)
    private String pinHash;

    // Getters & Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getPinHash() {
        return pinHash;
    }

    public void setPinHash(String pinHash) {
        this.pinHash = pinHash;
    }
}
