package com.cp.party_trip.repository;

import com.cp.party_trip.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExpenseRepo extends JpaRepository<Expense, Long> {
    List<Expense> findByTripId(Long tripId);

    // หาผลรวมแยกตามหมวดหมู่
    @org.springframework.data.jpa.repository.Query("SELECT e.category, SUM(e.totalAmount) FROM Expense e WHERE e.trip.id = :tripId GROUP BY e.category")
    java.util.List<Object[]> sumAmountByCategory(
            @org.springframework.data.repository.query.Param("tripId") Long tripId);

    // หาผลรวมแยกตามรายวัน
    @org.springframework.data.jpa.repository.Query(value = "SELECT DATE(expense_date) as exp_date, SUM(total_amount) FROM expenses WHERE trip_id = :tripId GROUP BY DATE(expense_date) ORDER BY exp_date ASC", nativeQuery = true)
    java.util.List<Object[]> sumAmountByDate(@org.springframework.data.repository.query.Param("tripId") Long tripId);
}