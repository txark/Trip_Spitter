package com.cp.party_trip.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.cp.party_trip.config.AuthGuard;
import com.cp.party_trip.dto.response.TripEventResponse;
import com.cp.party_trip.mapper.TripEventMapper;
import com.cp.party_trip.service.TripEventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Trip events - ความเคลื่อนไหวของทริป")
@RestController
@RequestMapping("/api/v1/trips/{tripId}/events")
public class TripEventController {
    private final TripEventService tripEventService;
    private final TripEventMapper tripEventMapper;
    private final AuthGuard guard;

    public TripEventController(TripEventService tripEventService, TripEventMapper tripEventMapper, AuthGuard guard) {
        this.tripEventService = tripEventService;
        this.tripEventMapper = tripEventMapper;
        this.guard = guard;
    }

    // ความเคลื่อนไหวล่าสุดของทริป (เฉพาะสมาชิก)
    @GetMapping
    public ResponseEntity<List<TripEventResponse>> recent(@PathVariable Long tripId) {
        guard.me(tripId);
        return ResponseEntity.ok(tripEventMapper.toResponses(tripEventService.recent(tripId)));
    }
}
