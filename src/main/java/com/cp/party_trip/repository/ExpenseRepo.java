package com.cp.party_trip.repository;

import com.cp.party_trip.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExpenseRepo extends JpaRepository<Expense, Long> {
    List<Expense> findByTripId(Long tripId);
}