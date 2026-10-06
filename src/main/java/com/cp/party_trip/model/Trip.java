package com.cp.party_trip.model;

import jakarta.persistence.*;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "trips")
public class Trip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title; 
    private LocalDate startDate;
    private LocalDate endDate;
    private String inviteCode;

    // เขตเวลาของที่เที่ยว (IANA เช่น Asia/Tokyo) ใช้บอก "ตอนนี้/ถัดไป" ในแพลน, null = เวลาไทย
    @Column(name = "time_zone", length = 50)
    private String timeZone;

    // งบที่ตั้งใจใช้ต่อคน (null = ยังไม่ตั้ง) เทียบกับยอดประมาณจากแพลน และยอดที่จ่ายจริง
    @Column(name = "budget_per_person", precision = 10, scale = 2)
    private java.math.BigDecimal budgetPerPerson;

    // สกุลเงินท้องถิ่นของที่เที่ยว (เช่น JPY) และเรท 1 หน่วย = กี่บาท (null = ใช้บาทอย่างเดียว)
    @Column(length = 3)
    private String currency;

    @Column(name = "exchange_rate", precision = 14, scale = 6)
    private java.math.BigDecimal exchangeRate;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

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

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public java.math.BigDecimal getExchangeRate() { return exchangeRate; }
    public void setExchangeRate(java.math.BigDecimal exchangeRate) { this.exchangeRate = exchangeRate; }

    public java.math.BigDecimal getBudgetPerPerson() { return budgetPerPerson; }
    public void setBudgetPerPerson(java.math.BigDecimal budgetPerPerson) { this.budgetPerPerson = budgetPerPerson; }

    public String getTimeZone() { return timeZone; }
    public void setTimeZone(String timeZone) { this.timeZone = timeZone; }

    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<TripMember> getTripMembers() { return tripMembers; }
    public void setTripMembers(List<TripMember> tripMembers) { this.tripMembers = tripMembers; }

    public List<Activity> getActivities() { return activities; }
    public void setActivities(List<Activity> activities) { this.activities = activities; }
}