package com.cp.party_trip.controller;

import com.cp.party_trip.dto.ActivityRequest;
import com.cp.party_trip.service.ActivityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    // แพลนทั้งทริป เรียงตามวันและเวลาแล้ว
    @GetMapping("/trip/{tripId}")
    public ResponseEntity<?> getActivitiesByTrip(@PathVariable Long tripId) {
        try {
            return ResponseEntity.ok(activityService.getTripActivities(tripId));
        } catch (ResponseStatusException e) {
            return error(e);
        }
    }

    @PostMapping("/add/{tripId}")
    public ResponseEntity<?> addActivity(@PathVariable Long tripId, @RequestBody ActivityRequest request) {
        try {
            return ResponseEntity.ok(activityService.addActivity(tripId, request));
        } catch (ResponseStatusException e) {
            return error(e);
        }
    }

    @PutMapping("/{activityId}")
    public ResponseEntity<?> updateActivity(@PathVariable Long activityId, @RequestBody ActivityRequest request) {
        try {
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
            activityService.deleteActivity(activityId, memberId);
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
