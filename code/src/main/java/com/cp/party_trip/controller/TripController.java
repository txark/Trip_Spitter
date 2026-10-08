package com.cp.party_trip.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.cp.party_trip.config.AuthGuard;
import com.cp.party_trip.dto.request.CreateTripRequest;
import com.cp.party_trip.dto.response.MemberResponse;
import com.cp.party_trip.dto.response.RecoveryCodeResponse;
import com.cp.party_trip.dto.response.TripResponse;
import com.cp.party_trip.mapper.MemberMapper;
import com.cp.party_trip.mapper.TripMapper;
import com.cp.party_trip.model.TripMember;
import com.cp.party_trip.service.TripService;
import com.cp.party_trip.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Tag(name = "Trips - ทริปและสมาชิก")
@RestController
@RequestMapping("/api/v1")
public class TripController {

    private final TripService tripService;
    private final UserService userService;
    private final TripMapper tripMapper;
    private final MemberMapper memberMapper;
    private final AuthGuard guard;
    // กดเข้าร่วมซ้ำพร้อมกันหลายเครื่อง: ทำทีละคำขอต่อ (รหัสเชิญ, ชื่อ) จะได้ไม่สร้างสมาชิกซ้ำ
    private final ConcurrentHashMap<String, Object> joinLocks = new ConcurrentHashMap<>();

    public TripController(TripService tripService, UserService userService, TripMapper tripMapper,
            MemberMapper memberMapper, AuthGuard guard) {
        this.tripService = tripService;
        this.userService = userService;
        this.tripMapper = tripMapper;
        this.memberMapper = memberMapper;
        this.guard = guard;
    }

    @GetMapping("/trips/{id}")
    public ResponseEntity<TripResponse> getTripById(@PathVariable Long id) {
        guard.me(id); // ข้อมูลทริปมีรหัสเชิญ: ดูได้เฉพาะสมาชิก
        return ResponseEntity.ok(tripMapper.toResponse(tripService.getTripById(id)));
    }

    // คนสร้าง = เจ้าของ token เสมอ (creatorName ที่หน้าเว็บส่งมาไม่ได้ใช้)
    @PostMapping("/trips")
    public ResponseEntity<TripResponse> createTrip(@Valid @RequestBody CreateTripRequest request,
            @RequestParam(required = false) String creatorName) {
        var created = tripService.createTrip(tripMapper.toEntity(request), guard.user().getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(tripMapper.toResponse(created));
    }

    @PostMapping("/invitations/{inviteCode}/members")
    public ResponseEntity<TripResponse> joinTrip(@PathVariable String inviteCode,
            @RequestParam(required = false) String memberName) {
        String name = guard.user().getUsername();
        String key = inviteCode.trim().toUpperCase() + "|" + name;
        TripMember joinedMember;
        synchronized (joinLocks.computeIfAbsent(key, k -> new Object())) {
            joinedMember = tripService.joinTrip(inviteCode, name);
        }
        return ResponseEntity.ok(tripMapper.toResponse(joinedMember.getTrip()));
    }

    @PutMapping("/trips/{tripId}/currency")
    public ResponseEntity<TripResponse> updateCurrency(@PathVariable Long tripId, @RequestParam Long memberId,
            @RequestParam(required = false) String currency, @RequestParam(required = false) BigDecimal rate) {
        guard.self(tripId, memberId);
        return ResponseEntity.ok(tripMapper.toResponse(tripService.updateCurrency(tripId, memberId, currency, rate)));
    }

    @PutMapping("/trips/{tripId}/budget")
    public ResponseEntity<TripResponse> updateBudget(@PathVariable Long tripId, @RequestParam Long memberId,
            @RequestParam(required = false) BigDecimal amount) {
        guard.self(tripId, memberId);
        return ResponseEntity.ok(tripMapper.toResponse(tripService.updateBudget(tripId, memberId, amount)));
    }

    // แก้วันเริ่ม/วันสิ้นสุด (รูปแบบ 2026-12-31)
    @PutMapping("/trips/{tripId}/dates")
    public ResponseEntity<TripResponse> updateDates(@PathVariable Long tripId, @RequestParam Long memberId,
            @RequestParam String startDate, @RequestParam String endDate) {
        guard.self(tripId, memberId);
        return ResponseEntity.ok(tripMapper.toResponse(tripService.updateDates(tripId, memberId, startDate, endDate)));
    }

    @PutMapping("/trips/{tripId}/timezone")
    public ResponseEntity<TripResponse> updateTimeZone(@PathVariable Long tripId, @RequestParam Long memberId,
            @RequestParam String timeZone) {
        guard.self(tripId, memberId);
        return ResponseEntity.ok(tripMapper.toResponse(tripService.updateTimeZone(tripId, memberId, timeZone)));
    }

    // คนสร้างทริปออกรหัสกู้คืนให้สมาชิกที่เปลี่ยนเครื่อง/ล้างเบราว์เซอร์ แล้วเข้าชื่อเดิมไม่ได้
    @PostMapping("/trips/{tripId}/members/{memberId}/recovery-codes")
    public ResponseEntity<RecoveryCodeResponse> recoveryCode(@PathVariable Long tripId, @PathVariable Long memberId) {
        TripMember me = guard.me(tripId);
        if (!"ADMIN".equals(me.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "เฉพาะคนสร้างทริปเท่านั้นที่ออกรหัสกู้คืนได้");
        }
        TripMember target = tripService.getMember(tripId, memberId);
        if (target.getId().equals(me.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ออกรหัสกู้คืนให้ตัวเองไม่ได้");
        }
        String code = userService.issueRecoveryCode(target.getGuestName());
        return ResponseEntity.ok(new RecoveryCodeResponse(target.getGuestName(), code,
                UserService.RECOVERY_TTL.toMinutes()));
    }

    @GetMapping("/trips/{tripId}/members")
    public ResponseEntity<List<MemberResponse>> getTripMembers(@PathVariable Long tripId) {
        guard.me(tripId);
        return ResponseEntity.ok(memberMapper.toResponses(tripService.getMembers(tripId)));
    }
}
