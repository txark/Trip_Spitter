package com.cp.party_trip.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

// ตั้งค่าของทริป (งบ สกุลเงิน เรท เขตเวลา) แยกจาก trips เป็นความสัมพันธ์ One-to-One
// เหตุผล: ข้อมูลหมวดนี้เปลี่ยนบ่อยและไม่ใช่ตัวตนของทริป (ชื่อ/วันที่/รหัสเชิญ) การแยกทำให้ trips เล็กลง
// และ trip_settings เป็นเจ้าของ FK (unique) จึงรับประกันได้ในระดับฐานข้อมูลว่า 1 ทริปมีได้ 1 ชุดตั้งค่า
@Entity
@Table(name = "trip_settings")
public class TripSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // LAZY: ฝั่งที่ถือ FK ไม่ต้องโหลดทริปทุกครั้งที่อ่านตั้งค่า
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false, unique = true)
    private Trip trip;

    // เขตเวลาของที่เที่ยว (IANA เช่น Asia/Tokyo) ใช้บอก "ตอนนี้/ถัดไป" ในแพลน, null = เวลาไทย
    @Column(name = "time_zone", length = 50)
    private String timeZone;

    // งบที่ตั้งใจใช้ต่อคน (null = ยังไม่ตั้ง)
    @Column(name = "budget_per_person", precision = 10, scale = 2)
    private BigDecimal budgetPerPerson;

    // สกุลเงินท้องถิ่นของที่เที่ยว (เช่น JPY) และเรท 1 หน่วย = กี่บาท (null = ใช้บาทอย่างเดียว)
    @Column(length = 3)
    private String currency;

    @Column(name = "exchange_rate", precision = 14, scale = 6)
    private BigDecimal exchangeRate;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Trip getTrip() { return trip; }
    public void setTrip(Trip trip) { this.trip = trip; }

    public String getTimeZone() { return timeZone; }
    public void setTimeZone(String timeZone) { this.timeZone = timeZone; }

    public BigDecimal getBudgetPerPerson() { return budgetPerPerson; }
    public void setBudgetPerPerson(BigDecimal budgetPerPerson) { this.budgetPerPerson = budgetPerPerson; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getExchangeRate() { return exchangeRate; }
    public void setExchangeRate(BigDecimal exchangeRate) { this.exchangeRate = exchangeRate; }
}
