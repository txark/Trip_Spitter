package com.cp.party_trip.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.cp.party_trip.config.AuthGuard;
import com.cp.party_trip.dto.request.ActivityRequest;
import com.cp.party_trip.dto.response.ActivityResponse;
import com.cp.party_trip.mapper.ActivityMapper;
import com.cp.party_trip.service.ActivityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Activities - แพลนของทริป")
@RestController
@RequestMapping("/api/v1/activities")
public class ActivityController {

    private final ActivityService activityService;
    private final ActivityMapper activityMapper;
    private final AuthGuard guard;

    public ActivityController(ActivityService activityService, ActivityMapper activityMapper, AuthGuard guard) {
        this.activityService = activityService;
        this.activityMapper = activityMapper;
        this.guard = guard;
    }

    // แพลนทั้งทริป เรียงตามวันและเวลาแล้ว
    @GetMapping("/trip/{tripId}")
    public ResponseEntity<List<ActivityResponse>> getActivitiesByTrip(@PathVariable Long tripId) {
        guard.me(tripId);
        return ResponseEntity.ok(activityMapper.toResponses(activityService.getTripActivities(tripId)));
    }

    // คนทำรายการ = เจ้าของ token (ส่ง memberId ของคนอื่นมา = 403)
    @PostMapping("/add/{tripId}")
    public ResponseEntity<ActivityResponse> addActivity(@PathVariable Long tripId,
            @Valid @RequestBody ActivityRequest request) {
        request.setMemberId(guard.self(tripId, request.getMemberId()).getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(activityMapper.toResponse(activityService.addActivity(tripId, request)));
    }

    @PutMapping("/{activityId}")
    public ResponseEntity<ActivityResponse> updateActivity(@PathVariable Long activityId,
            @Valid @RequestBody ActivityRequest request) {
        request.setMemberId(guard.self(guard.tripOfActivity(activityId), request.getMemberId()).getId());
        return ResponseEntity.ok(activityMapper.toResponse(activityService.updateActivity(activityId, request)));
    }

    @DeleteMapping("/{activityId}")
    public ResponseEntity<Void> deleteActivity(@PathVariable Long activityId,
            @RequestParam(required = false) Long memberId) {
        Long me = guard.self(guard.tripOfActivity(activityId), memberId).getId();
        activityService.deleteActivity(activityId, me);
        return ResponseEntity.noContent().build();
    }
}
