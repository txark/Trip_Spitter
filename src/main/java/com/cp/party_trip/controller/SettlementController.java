package com.cp.party_trip.controller;

import com.cp.party_trip.config.AuthGuard;
import com.cp.party_trip.dto.SettlementDTO;
import com.cp.party_trip.model.Settlement;
import com.cp.party_trip.model.Trip;
import com.cp.party_trip.model.TripMember;
import com.cp.party_trip.repository.SettlementRepo;
import com.cp.party_trip.repository.TripRepo;
import com.cp.party_trip.repository.TripMemberRepo;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/settlements")
public class SettlementController {

    private final SettlementRepo settlementRepo;
    private final TripRepo tripRepo;
    private final TripMemberRepo tripMemberRepo;

    private final AuthGuard guard;

    public SettlementController(SettlementRepo settlementRepo, TripRepo tripRepo, TripMemberRepo tripMemberRepo,
            AuthGuard guard) {
        this.guard = guard;
        this.settlementRepo = settlementRepo;
        this.tripRepo = tripRepo;
        this.tripMemberRepo = tripMemberRepo;
    }

    @PostMapping("/pay/{tripId}")
    public ResponseEntity<?> clearDebt(
            @PathVariable Long tripId,
            @RequestParam Long senderId,
            @RequestParam Long receiverId,
            @RequestParam BigDecimal amount) {

        // บันทึกได้เฉพาะคนที่เกี่ยวข้อง (คนจ่ายหรือคนรับ)
        Long me = guard.me(tripId).getId();
        if (!me.equals(senderId) && !me.equals(receiverId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("บันทึกได้เฉพาะคนจ่ายหรือคนรับเงิน");
        }
        if (amount == null || amount.signum() <= 0) {
            return ResponseEntity.badRequest().body("จำนวนเงินต้องมากกว่า 0");
        }
        if (senderId.equals(receiverId)) {
            return ResponseEntity.badRequest().body("ผู้จ่ายและผู้รับต้องเป็นคนละคนกัน");
        }
        Trip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ไม่พบทริปนี้"));
        TripMember sender = tripMemberRepo.findById(senderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ไม่พบผู้จ่าย"));
        TripMember receiver = tripMemberRepo.findById(receiverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ไม่พบผู้รับ"));
        if (sender.getTrip() == null || receiver.getTrip() == null
                || !tripId.equals(sender.getTrip().getId()) || !tripId.equals(receiver.getTrip().getId())) {
            return ResponseEntity.badRequest().body("ผู้จ่ายและผู้รับต้องอยู่ในทริปนี้");
        }

        Settlement settlement = new Settlement();
        settlement.setTrip(trip);
        settlement.setSender(sender);
        settlement.setReceiver(receiver);
        settlement.setAmount(amount.setScale(2, java.math.RoundingMode.HALF_UP));

        return ResponseEntity.ok(settlementRepo.save(settlement));
    }

    @GetMapping("/history/{tripId}")
    public ResponseEntity<List<SettlementDTO>> getSettlementHistory(@PathVariable Long tripId) {
        guard.me(tripId);
        List<Settlement> settlements = settlementRepo.findByTripId(tripId);
        List<SettlementDTO> response = new ArrayList<>();

        for (Settlement s : settlements) {
            response.add(new SettlementDTO(
                    s.getId(),
                    s.getSender().getGuestName(), // ใช้ getGuestName() ตาม Entity
                    s.getReceiver().getGuestName(), // ใช้ getGuestName() ตาม Entity
                    s.getAmount(),
                    s.getSettledAt()));
        }

        return ResponseEntity.ok(response);
    }
}