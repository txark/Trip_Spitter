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
}
