package com.cp.party_trip.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

// กิจกรรมในแพลนเที่ยว: 1 แถว = 1 กิจกรรมในวันหนึ่งของทริป
@Entity
@Table(name = "activities")
public class Activity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "trip_id")
    @JsonIgnore // ป้องกัน Infinite Recursion (Trip -> activities -> trip -> ...)
    private Trip trip;

    private String title;

    // ข้อมูลเก่าเก็บวัน+เวลาไว้ในช่องเดียว ตอนนี้ใช้ activityDate/startTime แทน แต่ยังอัปเดตให้ตรงกันเสมอ
    @JsonIgnore
    private LocalDateTime activityTime;

    private LocalDate activityDate;

    // null = ไม่ระบุเวลา (แสดงเป็น "ทั้งวัน" ไว้บนสุดของวัน)
    private LocalTime startTime;

    // น้อยกว่าเวลาเริ่ม = จบวันถัดไป (เช่น รถทัวร์ 22:00 → 06:00)
    private LocalTime endTime;

    @Column(length = 150)
    private String location;

    @Column(length = 500)
    private String notes;

    @Column(length = 20)
    private String category;

    // --- ช่องเฉพาะประเภท (ประเภทอื่นเป็น null) ---
    // ที่พัก: วันเช็คเอาท์ (startTime = เวลาเช็คอิน, endTime = เวลาเช็คเอาท์)
    private LocalDate endDate;

    // เดินทาง: ต้นทาง (ปลายทางใช้ location) และวิธีเดินทาง
    @Column(length = 150)
    private String origin;

    @Column(length = 20)
    private String transportMode;

    // กิน: มื้อ
    @Column(length = 20)
    private String mealType;

    // เดินทาง/ที่พัก/กิจกรรม: เลขการจอง เลขเที่ยว ที่นั่ง
    @Column(length = 100)
    private String bookingRef;

    // กิน/กิจกรรม: จองแล้วหรือยัง
    private Boolean booked;

    // พิกัดของ location (เลือกจากแผนที่) ใช้เปิด Google Maps ให้ตรงจุด
    private Double latitude;
    private Double longitude;

    // ที่พัก: จำนวนห้อง (cost = ราคาต่อห้องต่อคืน) และพักได้ห้องละกี่คน
    private Integer rooms;
    private Integer guestsPerRoom;

    // วิธีจอง: ONLINE (มีเลขการจอง) / PHONE (โทรจอง) / PAGE (จองผ่านเพจ/แชท), null = ยังไม่จอง
    // bookingRef เก็บรายละเอียดตามวิธีนั้น เช่น เลขการจอง เบอร์ที่โทร หรือชื่อเพจ
    @Column(length = 20)
    private String bookingMethod;

    // กิจกรรม: ผู้ให้บริการ / เบอร์ติดต่อ
    @Column(length = 100)
    private String contact;

    // กิจกรรม: สมาชิกที่ไปด้วย (ไม่ใช่ทุกคนจะไปทุกกิจกรรม)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "activity_participants", joinColumns = @JoinColumn(name = "activity_id"))
    @Column(name = "member_id")
    @OrderColumn(name = "position")
    private List<Long> participantIds = new ArrayList<>();

    // ค่าใช้จ่ายโดยประมาณ: ที่พัก = ต่อห้องต่อคืน, ประเภทอื่น = ต่อคน
    @Column(precision = 10, scale = 2)
    private BigDecimal cost;

    private Long createdByMemberId;

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // --- Getters & Setters ---
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getActivityTime() {
        return activityTime;
    }

    public void setActivityTime(LocalDateTime activityTime) {
        this.activityTime = activityTime;
    }

    public LocalDate getActivityDate() {
        if (activityDate == null && activityTime != null) {
            return activityTime.toLocalDate();
        }
        return activityDate;
    }

    public void setActivityDate(LocalDate activityDate) {
        this.activityDate = activityDate;
        syncActivityTime();
    }

    public LocalTime getStartTime() {
        if (activityDate == null && startTime == null && activityTime != null) {
            return activityTime.toLocalTime();
        }
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
        syncActivityTime();
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getTransportMode() {
        return transportMode;
    }

    public void setTransportMode(String transportMode) {
        this.transportMode = transportMode;
    }

    public String getMealType() {
        return mealType;
    }

    public void setMealType(String mealType) {
        this.mealType = mealType;
    }

    public String getBookingRef() {
        return bookingRef;
    }

    public void setBookingRef(String bookingRef) {
        this.bookingRef = bookingRef;
    }

    public Boolean getBooked() {
        return booked;
    }

    public void setBooked(Boolean booked) {
        this.booked = booked;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Integer getRooms() {
        return rooms;
    }

    public void setRooms(Integer rooms) {
        this.rooms = rooms;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public List<Long> getParticipantIds() {
        return participantIds;
    }

    public void setParticipantIds(List<Long> participantIds) {
        this.participantIds = participantIds == null ? new ArrayList<>() : new ArrayList<>(participantIds);
    }

    public String getBookingMethod() {
        return bookingMethod;
    }

    public void setBookingMethod(String bookingMethod) {
        this.bookingMethod = bookingMethod;
    }

    public Integer getGuestsPerRoom() {
        return guestsPerRoom;
    }

    public void setGuestsPerRoom(Integer guestsPerRoom) {
        this.guestsPerRoom = guestsPerRoom;
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

    private void syncActivityTime() {
        if (activityDate != null) {
            activityTime = activityDate.atTime(startTime != null ? startTime : LocalTime.MIDNIGHT);
        }
    }
}
