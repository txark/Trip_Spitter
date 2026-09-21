package com.cp.party_trip.repository;

import com.cp.party_trip.model.Poll;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PollRepo extends JpaRepository<Poll, Long> {
    List<Poll> findByTripId(Long tripId);
}