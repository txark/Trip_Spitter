package com.cp.party_trip.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// ข้อมูลทริป + สมาชิก
public record TripResponse(
        Long id,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        String inviteCode,
        String timeZone,
        BigDecimal budgetPerPerson,
        String currency,
        BigDecimal exchangeRate,
        LocalDateTime createdAt,
        List<MemberResponse> tripMembers) {
}
