package com.cp.party_trip.controller;

import com.cp.party_trip.dto.CategorySummaryDTO;
import com.cp.party_trip.dto.DailySummaryDTO;
import com.cp.party_trip.service.InsightService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/insights")
public class InsightController {

    private final InsightService insightService;

    public InsightController(InsightService insightService) {
        this.insightService = insightService;
    }

    @GetMapping("/{tripId}/categories")
    public ResponseEntity<List<CategorySummaryDTO>> getCategorySummary(@PathVariable Long tripId) {
        return ResponseEntity.ok(insightService.getCategoryInsights(tripId));
    }

    @GetMapping("/{tripId}/daily")
    public ResponseEntity<List<DailySummaryDTO>> getDailySummary(@PathVariable Long tripId) {
        return ResponseEntity.ok(insightService.getDailyInsights(tripId));
    }
}