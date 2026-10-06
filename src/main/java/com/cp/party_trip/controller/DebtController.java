package com.cp.party_trip.controller;

import com.cp.party_trip.config.AuthGuard;
import com.cp.party_trip.dto.response.DebtTransferResponse;
import com.cp.party_trip.dto.response.MemberDebtSummaryResponse;
import com.cp.party_trip.mapper.DebtMapper;
import com.cp.party_trip.service.DebtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/debts")
public class DebtController {

    private final DebtService debtService;
    private final DebtMapper debtMapper;
    private final AuthGuard guard;

    public DebtController(DebtService debtService, DebtMapper debtMapper, AuthGuard guard) {
        this.debtService = debtService;
        this.debtMapper = debtMapper;
        this.guard = guard;
    }

    // ใครต้องโอนให้ใคร (รวบยอดแล้ว)
    @GetMapping("/simplify/{tripId}")
    public ResponseEntity<List<DebtTransferResponse>> getSimplifiedDebts(@PathVariable Long tripId) {
        guard.me(tripId);
        return ResponseEntity.ok(debtMapper.toResponses(debtService.calculateDebtSimplification(tripId)));
    }

    // สรุปหนี้ "ของฉัน" (หนี้ที่ต้องจ่าย + บิลที่สำรองจ่าย + การรับเงินก้อน): ขอดูของสมาชิกคนอื่นไม่ได้
    @GetMapping("/summary-details/{tripId}")
    public ResponseEntity<MemberDebtSummaryResponse> getTripDebtSummary(@PathVariable Long tripId,
            @RequestParam Long userId) {
        guard.self(tripId, userId);
        return ResponseEntity.ok(debtService.getMemberSummary(tripId, userId));
    }
}
