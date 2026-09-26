package com.cp.party_trip.controller;

import com.cp.party_trip.dto.SettlementDTO;
import com.cp.party_trip.model.Settlement;
import com.cp.party_trip.model.Trip;
import com.cp.party_trip.model.TripMember;
import com.cp.party_trip.repository.SettlementRepo;
import com.cp.party_trip.repository.TripRepo;
import com.cp.party_trip.repository.TripMemberRepo;
import org.springframework.http.ResponseEntity;
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

    public SettlementController(SettlementRepo settlementRepo, TripRepo tripRepo, TripMemberRepo tripMemberRepo) {
        this.settlementRepo = settlementRepo;
        this.tripRepo = tripRepo;
        this.tripMemberRepo = tripMemberRepo;
    }

    @PostMapping("/pay/{tripId}")
    public ResponseEntity<Settlement> clearDebt(
            @PathVariable Long tripId,
            @RequestParam Long senderId,
            @RequestParam Long receiverId,
            @RequestParam BigDecimal amount) {

        Trip trip = tripRepo.findById(tripId).orElseThrow();
        TripMember sender = tripMemberRepo.findById(senderId).orElseThrow();
        TripMember receiver = tripMemberRepo.findById(receiverId).orElseThrow();

        Settlement settlement = new Settlement();
        settlement.setTrip(trip);
        settlement.setSender(sender);
        settlement.setReceiver(receiver);
        settlement.setAmount(amount);

        return ResponseEntity.ok(settlementRepo.save(settlement));
    }

    @GetMapping("/history/{tripId}")
    public ResponseEntity<List<SettlementDTO>> getSettlementHistory(@PathVariable Long tripId) {
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