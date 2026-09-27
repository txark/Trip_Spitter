package com.cp.party_trip.controller;

import com.cp.party_trip.model.Trip;
import com.cp.party_trip.model.TripMember;
import com.cp.party_trip.service.TripService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trips")
@CrossOrigin(origins = { "http://127.0.0.1:5500", "http://localhost:5500" })
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Trip> getTripById(@PathVariable Long id) {
        Trip trip = tripService.getTripById(id);
        return ResponseEntity.ok(trip);
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