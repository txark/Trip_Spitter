package com.cp.party_trip.dto.response;

// ข้อความผลลัพธ์/ข้อผิดพลาด รูปแบบเดียวกันทุก endpoint: {"message": "..."}
public record MessageResponse(String message) {
}
