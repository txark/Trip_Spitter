package com.cp.party_trip.controller;

import com.cp.party_trip.model.UserTripHistory;
import com.cp.party_trip.service.UserTripHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/history")
@CrossOrigin(origins = { "http://127.0.0.1:5500", "http://localhost:5500" }) // อนุญาตให้หน้าเว็บเรียกใช้งานได้
public class UserTripHistoryController {

    private final UserTripHistoryService historyService;

    public UserTripHistoryController(UserTripHistoryService historyService) {
        this.historyService = historyService;
    }

    // ดึงประวัติทริปล่าสุดตาม userId
    @GetMapping("/recent/{userId}")
    public ResponseEntity<List<UserTripHistory>> getRecentTrips(@PathVariable Long userId) {
        List<UserTripHistory> historyList = historyService.getRecentTrips(userId);
        return ResponseEntity.ok(historyList);
    }

    // บันทึกประวัติเมื่อผู้ใช้กดเข้าดูทริป
    @PostMapping("/view")
    public ResponseEntity<?> recordView(
            @RequestParam Long userId,
            @RequestParam Long tripId) {
        historyService.recordTripView(userId, tripId);
        return ResponseEntity.ok("บันทึกประวัติการเข้าชมสำเร็จ");
    }
}