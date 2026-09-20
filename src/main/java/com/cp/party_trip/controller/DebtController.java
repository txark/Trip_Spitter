package com.cp.party_trip.controller;

import com.cp.party_trip.dto.DebtTransfer;
import com.cp.party_trip.service.DebtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/debts")
public class DebtController {

    private final DebtService debtService;

    public DebtController(DebtService debtService) {
        this.debtService = debtService;
    }

    @GetMapping("/simplify/{tripId}")
    public ResponseEntity<List<DebtTransfer>> getSimplifiedDebts(@PathVariable Long tripId) {
        List<DebtTransfer> transfers = debtService.calculateDebtSimplification(tripId);
        return ResponseEntity.ok(transfers);
    }
}