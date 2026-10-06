package com.cp.party_trip.controller;

import com.cp.party_trip.config.AuthGuard;
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
    private final AuthGuard guard;

    public UserTripHistoryController(UserTripHistoryService historyService, AuthGuard guard) {
        this.historyService = historyService;
        this.guard = guard;
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
        // บันทึกได้เฉพาะประวัติของตัวเอง
        historyService.recordTripView(guard.user().getId(), tripId);
        return ResponseEntity.ok("บันทึกประวัติการเข้าชมสำเร็จ");
    }
}