package com.cp.party_trip.controller;

import com.cp.party_trip.config.AuthGuard;
import com.cp.party_trip.model.User;
import com.cp.party_trip.model.UserTripHistory;
import com.cp.party_trip.repository.TripMemberRepo;
import com.cp.party_trip.service.UserTripHistoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/history")
@CrossOrigin(origins = { "http://127.0.0.1:5500", "http://localhost:5500" }) // อนุญาตให้หน้าเว็บเรียกใช้งานได้
public class UserTripHistoryController {

    private final UserTripHistoryService historyService;
    private final AuthGuard guard;
    private final TripMemberRepo tripMemberRepo;

    public UserTripHistoryController(UserTripHistoryService historyService, AuthGuard guard,
            TripMemberRepo tripMemberRepo) {
        this.historyService = historyService;
        this.guard = guard;
        this.tripMemberRepo = tripMemberRepo;
    }

    // ประวัติทริปล่าสุดของเจ้าของ token (userId ในลิงก์ต้องเป็นตัวเอง)
    @GetMapping("/recent/{userId}")
    public ResponseEntity<List<UserTripHistory>> getRecentTrips(@PathVariable Long userId) {
        User me = guard.user();
        if (!me.getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<UserTripHistory> historyList = historyService.getRecentTrips(userId).stream()
                .filter(h -> tripMemberRepo
                        .findFirstByTripIdAndGuestNameOrderByIdAsc(h.getTripId(), me.getUsername()).isPresent())
                .toList();
        return ResponseEntity.ok(historyList);
    }

    // บันทึกประวัติเมื่อผู้ใช้กดเข้าดูทริป
    @PostMapping("/view")
    public ResponseEntity<?> recordView(
            @RequestParam Long userId,
            @RequestParam Long tripId) {
        // บันทึกได้เฉพาะประวัติของตัวเอง และเฉพาะทริปที่เป็นสมาชิก
        guard.me(tripId);
        historyService.recordTripView(guard.user().getId(), tripId);
        return ResponseEntity.ok("บันทึกประวัติการเข้าชมสำเร็จ");
    }
}