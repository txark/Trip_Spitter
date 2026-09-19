package com.cp.party_trip.controller;

import com.cp.party_trip.model.Activity;
import com.cp.party_trip.repository.ActivityRepo;
import com.cp.party_trip.repository.TripRepo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    private final ActivityRepo activityRepo;
    private final TripRepo tripRepo;

    public ActivityController(ActivityRepo activityRepo, TripRepo tripRepo) {
        this.activityRepo = activityRepo;
        this.tripRepo = tripRepo;
    }

    // API add a new activity to a trip
    @PostMapping("/add/{tripId}")
    public ResponseEntity<Activity> addActivity(@PathVariable Long tripId, @RequestBody Activity activity) {
        return tripRepo.findById(tripId).map(trip -> {
            activity.setTrip(trip);
            Activity savedActivity = activityRepo.save(activity);
            return ResponseEntity.ok(savedActivity);
        }).orElse(ResponseEntity.notFound().build());
    }

    // API get all activities for a specific trip
    @GetMapping("/trip/{tripId}")
    public ResponseEntity<List<Activity>> getActivitiesByTrip(@PathVariable Long tripId) {
        List<Activity> activities = activityRepo.findByTripId(tripId);
        return ResponseEntity.ok(activities);
    }
}