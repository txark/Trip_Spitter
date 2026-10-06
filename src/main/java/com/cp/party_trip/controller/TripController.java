package com.cp.party_trip.controller;

import com.cp.party_trip.config.AuthGuard;
import com.cp.party_trip.model.Trip;
import com.cp.party_trip.model.TripMember;
import com.cp.party_trip.repository.TripMemberRepo;
import com.cp.party_trip.service.TripService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/trips")
@CrossOrigin(origins = { "http://127.0.0.1:5500", "http://localhost:5500" })
public class TripController {

    private final TripService tripService;
    private final TripMemberRepo tripMemberRepo;
    private final AuthGuard guard;

    public TripController(TripService tripService, TripMemberRepo tripMemberRepo, AuthGuard guard) {
        this.tripService = tripService;
        this.tripMemberRepo = tripMemberRepo;
        this.guard = guard;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Trip> getTripById(@PathVariable Long id) {
        Trip trip = tripService.getTripById(id);
        return ResponseEntity.ok(trip);
    }

    @PostMapping("/create")
    public ResponseEntity<Trip> createTrip(@RequestBody Trip trip,
            @RequestParam(required = false) String creatorName) {
        // คนสร้าง = เจ้าของ token เสมอ (ไม่ใช้ชื่อที่หน้าเว็บส่งมา)
        Trip createdTrip = tripService.createTrip(trip, guard.user().getUsername());
        return ResponseEntity.ok(createdTrip);
    }

    @PostMapping("/join/{inviteCode}")
    public ResponseEntity<Trip> joinTrip(@PathVariable String inviteCode,
            @RequestParam(required = false) String memberName) {
        TripMember joinedMember = tripService.joinTrip(inviteCode, guard.user().getUsername());
        // ส่งข้อมูล Trip กลับไปตรงๆ ให้หน้าเว็บนำไปใช้งานต่อได้ทันที
        return ResponseEntity.ok(joinedMember.getTrip());
    }

    @PutMapping("/{tripId}/currency")
    public ResponseEntity<Trip> updateCurrency(@PathVariable Long tripId, @RequestParam Long memberId,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) java.math.BigDecimal rate) {
        guard.self(tripId, memberId);
        return ResponseEntity.ok(tripService.updateCurrency(tripId, memberId, currency, rate));
    }

    @PutMapping("/{tripId}/budget")
    public ResponseEntity<Trip> updateBudget(@PathVariable Long tripId, @RequestParam Long memberId,
            @RequestParam(required = false) java.math.BigDecimal amount) {
        guard.self(tripId, memberId);
        return ResponseEntity.ok(tripService.updateBudget(tripId, memberId, amount));
    }

    @PutMapping("/{tripId}/timezone")
    public ResponseEntity<Trip> updateTimeZone(@PathVariable Long tripId, @RequestParam Long memberId,
            @RequestParam String timeZone) {
        guard.self(tripId, memberId);
        return ResponseEntity.ok(tripService.updateTimeZone(tripId, memberId, timeZone));
    }

    // ส่งข้อความไทยกลับเป็น {"message": ...} ทุกสถานะ (error ของ Spring เองไม่มี message ให้หน้าเว็บแสดง)
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> error(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode())
                .body(Map.of("message", e.getReason() == null ? "" : e.getReason()));
    }

    @GetMapping("/{tripId}/members")
    public ResponseEntity<java.util.List<TripMember>> getTripMembers(@PathVariable Long tripId) {
        java.util.List<TripMember> members = tripMemberRepo.findByTripId(tripId);
        return ResponseEntity.ok(members);
    }
}