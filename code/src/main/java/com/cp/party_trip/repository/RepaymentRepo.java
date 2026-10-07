package com.cp.party_trip.repository;

import com.cp.party_trip.model.Repayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepaymentRepo extends JpaRepository<Repayment, Long> {
    List<Repayment> findByTripIdOrderByCreatedAtAsc(Long tripId);
}
