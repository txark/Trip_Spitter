package com.cp.party_trip.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.cp.party_trip.model.TripMember;

public interface TripMemberRepo extends JpaRepository<TripMember, Long> {
}