package com.cp.party_trip.dto.response;

// ผลการเข้าสู่ระบบ/เปลี่ยนชื่อ: ผู้ใช้ + token ที่หน้าเว็บต้องเก็บไว้
public record LoginResponse(Long id, String username, boolean pinSet, String token) {
}
