package com.cp.party_trip.controller;

import com.cp.party_trip.config.AuthGuard;
import com.cp.party_trip.dto.DebtTransfer;
import com.cp.party_trip.service.DebtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/debts")
@CrossOrigin(origins = { "http://127.0.0.1:5500", "http://localhost:5500" })
public class DebtController {

    private final DebtService debtService;
    private final AuthGuard guard;

    public DebtController(DebtService debtService, AuthGuard guard) {
        this.guard = guard;
        this.debtService = debtService;
    }

    @GetMapping("/simplify/{tripId}")
    public ResponseEntity<List<DebtTransfer>> getSimplifiedDebts(@PathVariable Long tripId) {
        guard.me(tripId);
        List<DebtTransfer> transfers = debtService.calculateDebtSimplification(tripId);
        return ResponseEntity.ok(transfers);
    }

    // สรุปหนี้ "ของฉัน" (หนี้ที่ต้องจ่าย + บิลที่สำรองจ่าย + การรับเงินก้อน): ขอดูของสมาชิกคนอื่นไม่ได้
    @GetMapping("/summary-details/{tripId}")
    public ResponseEntity<Map<String, Object>> getTripDebtSummary(@PathVariable Long tripId,
            @RequestParam Long userId) {
        guard.self(tripId, userId);
        return ResponseEntity.ok(debtService.getMemberSummary(tripId, userId));
    }
}
