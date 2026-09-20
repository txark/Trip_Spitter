package com.cp.party_trip.repository;

import com.cp.party_trip.model.ExpenseSplit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseSplitRepo extends JpaRepository<ExpenseSplit, Long> {
}