package com.cp.party_trip.controller;

import com.cp.party_trip.model.Trip;
import com.cp.party_trip.model.TripMember;
import com.cp.party_trip.service.TripService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @PostMapping("/create")
    public ResponseEntity<Trip> createTrip(@RequestBody Trip trip, @RequestParam String creatorName) {
        Trip createdTrip = tripService.createTrip(trip, creatorName);
        return ResponseEntity.ok(createdTrip);
    }

    @PostMapping("/join/{inviteCode}")
    public ResponseEntity<TripMember> joinTrip(@PathVariable String inviteCode, @RequestParam String memberName) {
        TripMember joinedMember = tripService.joinTrip(inviteCode, memberName);
        return ResponseEntity.ok(joinedMember);
    }
}