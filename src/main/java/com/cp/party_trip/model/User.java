package com.cp.party_trip.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
// ชื่อเล่นห้ามซ้ำ: สมัครชื่อเดียวกันพร้อมกัน 2 เครื่องเคยได้ 2 บัญชี แล้วเข้าชื่อนั้นไม่ได้อีกเลย
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_username", columnNames = "username"))
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

    // รหัสกู้คืนที่คนสร้างทริปออกให้ (เปลี่ยนเครื่อง/ล้างเบราว์เซอร์แล้วไม่มี PIN) ใช้ได้ครั้งเดียว มีวันหมดอายุ
    @JsonIgnore
    @Column(name = "recovery_hash", length = 120)
    private String recoveryHash;

    @JsonIgnore
    @Column(name = "recovery_expires_at")
    private LocalDateTime recoveryExpiresAt;

    public String getRecoveryHash() {
        return recoveryHash;
    }

    public void setRecoveryHash(String recoveryHash) {
        this.recoveryHash = recoveryHash;
    }

    public LocalDateTime getRecoveryExpiresAt() {
        return recoveryExpiresAt;
    }

    public void setRecoveryExpiresAt(LocalDateTime recoveryExpiresAt) {
        this.recoveryExpiresAt = recoveryExpiresAt;
    }

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
