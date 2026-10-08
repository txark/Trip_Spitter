package com.cp.party_trip.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.cp.party_trip.config.AuthGuard;
import com.cp.party_trip.dto.response.MessageResponse;
import com.cp.party_trip.dto.response.TripHistoryResponse;
import com.cp.party_trip.mapper.TripMapper;
import com.cp.party_trip.model.User;
import com.cp.party_trip.service.UserTripHistoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Tag(name = "History - ทริปที่เปิดล่าสุด")
@RestController
@RequestMapping("/api/v1/users/{userId}/trip-history")
public class UserTripHistoryController {

    private final UserTripHistoryService historyService;
    private final TripMapper tripMapper;
    private final AuthGuard guard;

    public UserTripHistoryController(UserTripHistoryService historyService, TripMapper tripMapper, AuthGuard guard) {
        this.historyService = historyService;
        this.tripMapper = tripMapper;
        this.guard = guard;
    }

    // ประวัติทริปล่าสุดของเจ้าของ token (userId ในลิงก์ต้องเป็นตัวเอง)
    @GetMapping
    public ResponseEntity<List<TripHistoryResponse>> getRecentTrips(@PathVariable Long userId) {
        User me = guard.user();
        if (!me.getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ดูได้เฉพาะประวัติของตัวเอง");
        }
        return ResponseEntity.ok(tripMapper.toHistoryResponses(historyService.getRecentTrips(me)));
    }

    // บันทึกประวัติเมื่อผู้ใช้กดเข้าดูทริป (เฉพาะของตัวเอง และเฉพาะทริปที่เป็นสมาชิก)
    @PutMapping("/{tripId}")
    public ResponseEntity<MessageResponse> recordView(@PathVariable Long userId, @PathVariable Long tripId) {
        guard.me(tripId);
        User me = guard.user();
        if (!me.getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "บันทึกได้เฉพาะประวัติของตัวเอง");
        }
        historyService.recordTripView(me.getId(), tripId);
        return ResponseEntity.ok(new MessageResponse("บันทึกประวัติการเข้าชมสำเร็จ"));
    }
}
