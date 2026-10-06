package com.cp.party_trip.controller;

import com.cp.party_trip.model.UserTripHistory;
import com.cp.party_trip.service.UserTripHistoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/history")
public class UserTripHistoryController {

    private final UserTripHistoryService historyService;

    public UserTripHistoryController(UserTripHistoryService historyService) {
        this.historyService = historyService;
    }

    // ประวัติทริปล่าสุดของเจ้าของ token (userId ในลิงก์ต้องเป็นตัวเอง)
    @GetMapping("/recent/{userId}")
    public ResponseEntity<List<UserTripHistory>> getRecentTrips(@PathVariable Long userId) {
        List<UserTripHistory> historyList = historyService.getRecentTrips(userId);
        return ResponseEntity.ok(historyList);
    }

    // บันทึกประวัติเมื่อผู้ใช้กดเข้าดูทริป (เฉพาะของตัวเอง
    // และเฉพาะทริปที่เป็นสมาชิก)
    @PostMapping("/view")
    public ResponseEntity<?> recordView(
            @RequestParam Long userId,
            @RequestParam Long tripId) {
        historyService.recordTripView(userId, tripId);
        return ResponseEntity.ok("บันทึกประวัติการเข้าชมสำเร็จ");
    }
}
