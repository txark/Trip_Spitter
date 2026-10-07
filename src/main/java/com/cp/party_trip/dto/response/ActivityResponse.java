package com.cp.party_trip.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;

// กิจกรรมในแพลน (ช่องที่ใช้ขึ้นกับประเภท category)
public record ActivityResponse(
        Long id,
        String title,
        String category,
        LocalDate activityDate,
        LocalDate endDate,
        LocalTime startTime,
        LocalTime endTime,
        String location,
        Double latitude,
        Double longitude,
        String notes,
        String origin,
        String transportMode,
        List<Stop> stops,
        String mealType,
        Integer rooms,
        Integer guestsPerRoom,
        String bookingMethod,
        String bookingRef,
        Boolean booked,
        String contact,
        BigDecimal cost,
        String costCurrency,
        BigDecimal costRate,
        List<Long> participantIds,
        Long pollId,
        Long createdByMemberId,
        LocalDateTime createdAt) {

    // จุดแวะ/เมืองต่อเครื่อง
    public record Stop(String place, LocalTime arriveTime, LocalTime departTime) {
    }

    // Builder Pattern: record นี้มี 28 ช่อง ถ้าใช้ constructor ตรงๆ ต้องเรียงอาร์กิวเมนต์ให้ถูกทุกตำแหน่ง
    // (ช่องชนิดเดียวกันอยู่ติดกันหลายคู่ เช่น startTime/endTime, latitude/longitude สลับแล้วคอมไพล์ผ่านแต่ข้อมูลผิด)
    // Builder ระบุชื่อช่องทุกครั้ง และช่องที่ไม่ใช้เว้นได้
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long id;
        private String title;
        private String category;
        private LocalDate activityDate;
        private LocalDate endDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private String location;
        private Double latitude;
        private Double longitude;
        private String notes;
        private String origin;
        private String transportMode;
        private List<Stop> stops;
        private String mealType;
        private Integer rooms;
        private Integer guestsPerRoom;
        private String bookingMethod;
        private String bookingRef;
        private Boolean booked;
        private String contact;
        private BigDecimal cost;
        private String costCurrency;
        private BigDecimal costRate;
        private List<Long> participantIds;
        private Long pollId;
        private Long createdByMemberId;
        private LocalDateTime createdAt;

        private Builder() {
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder activityDate(LocalDate activityDate) {
            this.activityDate = activityDate;
            return this;
        }

        public Builder endDate(LocalDate endDate) {
            this.endDate = endDate;
            return this;
        }

        public Builder startTime(LocalTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder endTime(LocalTime endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder location(String location) {
            this.location = location;
            return this;
        }

        public Builder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public Builder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public Builder origin(String origin) {
            this.origin = origin;
            return this;
        }

        public Builder transportMode(String transportMode) {
            this.transportMode = transportMode;
            return this;
        }

        public Builder stops(List<Stop> stops) {
            this.stops = stops;
            return this;
        }

        public Builder mealType(String mealType) {
            this.mealType = mealType;
            return this;
        }

        public Builder rooms(Integer rooms) {
            this.rooms = rooms;
            return this;
        }

        public Builder guestsPerRoom(Integer guestsPerRoom) {
            this.guestsPerRoom = guestsPerRoom;
            return this;
        }

        public Builder bookingMethod(String bookingMethod) {
            this.bookingMethod = bookingMethod;
            return this;
        }

        public Builder bookingRef(String bookingRef) {
            this.bookingRef = bookingRef;
            return this;
        }

        public Builder booked(Boolean booked) {
            this.booked = booked;
            return this;
        }

        public Builder contact(String contact) {
            this.contact = contact;
            return this;
        }

        public Builder cost(BigDecimal cost) {
            this.cost = cost;
            return this;
        }

        public Builder costCurrency(String costCurrency) {
            this.costCurrency = costCurrency;
            return this;
        }

        public Builder costRate(BigDecimal costRate) {
            this.costRate = costRate;
            return this;
        }

        public Builder participantIds(List<Long> participantIds) {
            this.participantIds = participantIds;
            return this;
        }

        public Builder pollId(Long pollId) {
            this.pollId = pollId;
            return this;
        }

        public Builder createdByMemberId(Long createdByMemberId) {
            this.createdByMemberId = createdByMemberId;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public ActivityResponse build() {
            return new ActivityResponse(
                    id,
                    title,
                    category,
                    activityDate,
                    endDate,
                    startTime,
                    endTime,
                    location,
                    latitude,
                    longitude,
                    notes,
                    origin,
                    transportMode,
                    stops,
                    mealType,
                    rooms,
                    guestsPerRoom,
                    bookingMethod,
                    bookingRef,
                    booked,
                    contact,
                    cost,
                    costCurrency,
                    costRate,
                    participantIds,
                    pollId,
                    createdByMemberId,
                    createdAt);
        }
    }
}
