package com.cp.party_trip.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.cp.party_trip.model.TripMember;
import java.util.List;
import java.util.Optional;

public interface TripMemberRepo extends JpaRepository<TripMember, Long> {
    List<TripMember> findByTripId(Long tripId);

    // ใช้ First กันข้อมูลเก่าที่มีชื่อซ้ำในทริปเดียวกัน (Optional ธรรมดาจะ error เมื่อเจอหลายแถว)
    Optional<TripMember> findFirstByTripIdAndGuestNameOrderByIdAsc(Long tripId, String guestName);
}