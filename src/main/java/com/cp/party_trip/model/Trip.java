package com.cp.party_trip.model;

import jakarta.persistence.*;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "trips", indexes = @Index(name = "idx_trips_invite_code", columnList = "invite_code"))
public class Trip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title; 
    private LocalDate startDate;
    private LocalDate endDate;
    @Column(name = "invite_code")
    private String inviteCode;

    // ตั้งค่าของทริป (งบ/สกุลเงิน/เรท/เขตเวลา) One-to-One
    // cascade ALL + orphanRemoval: ตั้งค่าไม่มีความหมายถ้าไม่มีทริป บันทึก/ลบทริปแล้วตามไปด้วยโดยไม่ต้องเรียก repo แยก
    // EAGER: ทุกหน้าที่โหลดทริปต้องใช้ค่าพวกนี้ (สกุลเงิน/เขตเวลา) และเป็นแถวเดียวต่อทริป จึงไม่คุ้มที่จะ lazy
    @OneToOne(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private TripSettings settings;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // LAZY (ค่าเริ่มต้นของ OneToMany): รายชื่อ/กิจกรรมโหลดเมื่อใช้เท่านั้น ไม่ต้องดึงมาทุกครั้งที่อ่านทริป
    // cascade ALL + orphanRemoval: สมาชิกและกิจกรรมเป็นส่วนหนึ่งของทริป ลบทริปแล้วต้องลบตาม
    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TripMember> tripMembers;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Activity> activities;

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    // getter/setter ของตั้งค่าส่งต่อไปที่ TripSettings เพื่อให้ service/mapper ที่ใช้อยู่ไม่ต้องรู้ว่าแยกตาราง
    public String getCurrency() { return settings == null ? null : settings.getCurrency(); }
    public void setCurrency(String currency) { settings().setCurrency(currency); }

    public java.math.BigDecimal getExchangeRate() { return settings == null ? null : settings.getExchangeRate(); }
    public void setExchangeRate(java.math.BigDecimal exchangeRate) { settings().setExchangeRate(exchangeRate); }

    public java.math.BigDecimal getBudgetPerPerson() { return settings == null ? null : settings.getBudgetPerPerson(); }
    public void setBudgetPerPerson(java.math.BigDecimal budgetPerPerson) { settings().setBudgetPerPerson(budgetPerPerson); }

    public String getTimeZone() { return settings == null ? null : settings.getTimeZone(); }
    public void setTimeZone(String timeZone) { settings().setTimeZone(timeZone); }

    public TripSettings getSettings() { return settings; }
    public void setSettings(TripSettings settings) { this.settings = settings; }

    private TripSettings settings() {
        if (settings == null) {
            settings = new TripSettings();
            settings.setTrip(this);
        }
        return settings;
    }

    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<TripMember> getTripMembers() { return tripMembers; }
    public void setTripMembers(List<TripMember> tripMembers) { this.tripMembers = tripMembers; }

    public List<Activity> getActivities() { return activities; }
    public void setActivities(List<Activity> activities) { this.activities = activities; }
}