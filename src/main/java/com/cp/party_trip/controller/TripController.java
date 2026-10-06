package com.cp.party_trip.controller;

import com.cp.party_trip.config.AuthGuard;
import com.cp.party_trip.model.Trip;
import com.cp.party_trip.model.TripMember;
import com.cp.party_trip.repository.TripMemberRepo;
import com.cp.party_trip.service.TripService;
import com.cp.party_trip.service.UserService;
import org.springframework.http.HttpStatus;
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
    private final UserService userService;
    // กดเข้าร่วมซ้ำพร้อมกันหลายเครื่อง: ทำทีละคำขอต่อ (รหัสเชิญ, ชื่อ) จะได้ไม่สร้างสมาชิกซ้ำ
    private final java.util.concurrent.ConcurrentHashMap<String, Object> joinLocks =
            new java.util.concurrent.ConcurrentHashMap<>();

    public TripController(TripService tripService, TripMemberRepo tripMemberRepo, AuthGuard guard,
            UserService userService) {
        this.userService = userService;
        this.tripService = tripService;
        this.tripMemberRepo = tripMemberRepo;
        this.guard = guard;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Trip> getTripById(@PathVariable Long id) {
        guard.me(id); // ข้อมูลทริปมีรหัสเชิญ: ดูได้เฉพาะสมาชิก
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
        String name = guard.user().getUsername();
        String key = inviteCode.trim().toUpperCase() + "|" + name;
        TripMember joinedMember;
        synchronized (joinLocks.computeIfAbsent(key, k -> new Object())) {
            joinedMember = tripService.joinTrip(inviteCode, name);
        }
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

    // แก้วันเริ่ม/วันสิ้นสุด (รูปแบบ 2026-12-31)
    @PutMapping("/{tripId}/dates")
    public ResponseEntity<Trip> updateDates(@PathVariable Long tripId, @RequestParam Long memberId,
            @RequestParam String startDate, @RequestParam String endDate) {
        guard.self(tripId, memberId);
        return ResponseEntity.ok(tripService.updateDates(tripId, memberId, startDate, endDate));
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

    // คนสร้างทริปออกรหัสกู้คืนให้สมาชิกที่เปลี่ยนเครื่อง/ล้างเบราว์เซอร์ แล้วเข้าชื่อเดิมไม่ได้
    @PostMapping("/{tripId}/members/{memberId}/recovery")
    public ResponseEntity<Map<String, Object>> recoveryCode(@PathVariable Long tripId, @PathVariable Long memberId) {
        TripMember me = guard.me(tripId);
        if (!"ADMIN".equals(me.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "เฉพาะคนสร้างทริปเท่านั้นที่ออกรหัสกู้คืนได้");
        }
        TripMember target = tripMemberRepo.findById(memberId)
                .filter(m -> m.getTrip() != null && tripId.equals(m.getTrip().getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ไม่พบสมาชิกคนนี้ในทริป"));
        if (target.getId().equals(me.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ออกรหัสกู้คืนให้ตัวเองไม่ได้");
        }
        String code = userService.issueRecoveryCode(target.getGuestName());
        return ResponseEntity.ok(Map.of("name", target.getGuestName(), "code", code,
                "expiresInMinutes", UserService.RECOVERY_TTL.toMinutes()));
    }

    @GetMapping("/{tripId}/members")
    public ResponseEntity<java.util.List<TripMember>> getTripMembers(@PathVariable Long tripId) {
        guard.me(tripId);
        java.util.List<TripMember> members = tripMemberRepo.findByTripId(tripId);
        return ResponseEntity.ok(members);
    }
}