package com.cp.party_trip.dto.response;

import java.time.LocalDateTime;

public record TripEventResponse(Long id, Long memberId, String type, String message, LocalDateTime createdAt) {
}
