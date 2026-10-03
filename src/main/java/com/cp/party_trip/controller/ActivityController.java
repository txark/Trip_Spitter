package com.cp.party_trip.controller;

import com.cp.party_trip.config.AuthGuard;
import com.cp.party_trip.dto.ActivityRequest;
import com.cp.party_trip.service.ActivityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    private final ActivityService activityService;
    private final AuthGuard guard;

    public ActivityController(ActivityService activityService, AuthGuard guard) {
        this.activityService = activityService;
        this.guard = guard;
    }

    // แพลนทั้งทริป เรียงตามวันและเวลาแล้ว
    @GetMapping("/trip/{tripId}")
    public ResponseEntity<?> getActivitiesByTrip(@PathVariable Long tripId) {
        try {
            guard.me(tripId);
            return ResponseEntity.ok(activityService.getTripActivities(tripId));
        } catch (ResponseStatusException e) {
            return error(e);
        }
    }

    @PostMapping("/add/{tripId}")
    public ResponseEntity<?> addActivity(@PathVariable Long tripId, @RequestBody ActivityRequest request) {
        try {
            // คนทำรายการ = เจ้าของ token (ส่ง memberId ของคนอื่นมา = 403)
            request.setMemberId(guard.self(tripId, request.getMemberId()).getId());
            return ResponseEntity.ok(activityService.addActivity(tripId, request));
        } catch (ResponseStatusException e) {
            return error(e);
        }
    }

    @PutMapping("/{activityId}")
    public ResponseEntity<?> updateActivity(@PathVariable Long activityId, @RequestBody ActivityRequest request) {
        try {
            request.setMemberId(guard.self(guard.tripOfActivity(activityId), request.getMemberId()).getId());
            return ResponseEntity.ok(activityService.updateActivity(activityId, request));
        } catch (ResponseStatusException e) {
            return error(e);
        }
    }

    @DeleteMapping("/{activityId}")
    public ResponseEntity<?> deleteActivity(
            @PathVariable Long activityId,
            @RequestParam(required = false) Long memberId) {
        try {
            Long me = guard.self(guard.tripOfActivity(activityId), memberId).getId();
            activityService.deleteActivity(activityId, me);
            return ResponseEntity.ok("ลบกิจกรรมสำเร็จ");
        } catch (ResponseStatusException e) {
            return error(e);
        }
    }

    // ส่งข้อความภาษาไทยกลับไปให้หน้าเว็บแสดงได้ตรง ๆ
    private ResponseEntity<String> error(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
    }
}
