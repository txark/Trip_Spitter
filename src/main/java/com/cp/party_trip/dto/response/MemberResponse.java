package com.cp.party_trip.dto.response;

import java.time.LocalDateTime;

// สมาชิกในทริป
public record MemberResponse(Long id, String guestName, String userName, String role, LocalDateTime joinedAt) {
}
