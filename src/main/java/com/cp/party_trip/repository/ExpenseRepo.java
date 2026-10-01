package com.cp.party_trip.repository;

import com.cp.party_trip.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ExpenseRepo extends JpaRepository<Expense, Long> {

    // ใช้ @Query เพื่อค้นหาผ่านความสัมพันธ์ของออบเจกต์ Trip
    @Query("SELECT e FROM Expense e WHERE e.trip.id = :tripId")
    List<Expense> findByTripId(@Param("tripId") Long tripId);

    // บิลที่บันทึกจากรายการในแพลน (ใช้ตอนลบรายการ)
    List<Expense> findByActivityId(Long activityId);

    // หาผลรวมแยกตามหมวดหมู่
    @Query("SELECT e.category, SUM(e.totalAmount) FROM Expense e WHERE e.trip.id = :tripId GROUP BY e.category")
    List<Object[]> sumAmountByCategory(@Param("tripId") Long tripId);

    // หาผลรวมแยกตามรายวัน
    @Query(value = "SELECT DATE(expense_date) as exp_date, SUM(total_amount) FROM expenses WHERE trip_id = :tripId GROUP BY DATE(expense_date) ORDER BY exp_date ASC", nativeQuery = true)
    List<Object[]> sumAmountByDate(@Param("tripId") Long tripId);
}