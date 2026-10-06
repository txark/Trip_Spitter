package com.cp.party_trip.dto.response;

// ข้อมูลผู้ใช้ที่หน้าเว็บเห็น (ไม่มี token/PIN)
public record UserResponse(Long id, String username, boolean pinSet) {
}
