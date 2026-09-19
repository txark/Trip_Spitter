package com.cp.party_trip.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.cp.party_trip.model.Trip;
import java.util.Optional;

public interface TripRepo extends JpaRepository<Trip, Long> {
    Optional<Trip> findByInviteCode(String inviteCode);
}
