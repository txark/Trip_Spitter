package com.cp.party_trip.dto.response;

// รหัสกู้คืนที่คนสร้างทริปออกให้เพื่อน
public record RecoveryCodeResponse(String name, String code, long expiresInMinutes) {
}
