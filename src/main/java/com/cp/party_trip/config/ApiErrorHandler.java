package com.cp.party_trip.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

// ทุกคอนโทรลเลอร์: error ที่โยนออกมา (เช่น 401/403 จาก AuthGuard) ส่งเป็น {"message": ...}
// error ของ Spring เองไม่มี message ให้หน้าเว็บแสดง (คอนโทรลเลอร์ที่มี @ExceptionHandler ของตัวเองยังใช้ของตัวเองก่อน)
@RestControllerAdvice
public class ApiErrorHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> error(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode())
                .body(Map.of("message", e.getReason() == null ? "" : e.getReason()));
    }
}
