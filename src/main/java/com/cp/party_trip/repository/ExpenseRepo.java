package com.cp.party_trip.repository;

import com.cp.party_trip.model.Expense;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepo extends JpaRepository<Expense, Long> {

    // ใช้ @Query เพื่อค้นหาผ่านความสัมพันธ์ของออบเจกต์ Trip
    @Query("SELECT e FROM Expense e WHERE e.trip.id = :tripId")
    List<Expense> findByTripId(@Param("tripId") Long tripId);

    // ล็อกบิลไว้จนจบ transaction: แก้/ลบบิลเดียวกันพร้อมกันหลายเครื่องจะต่อคิวกัน
    // ไม่ชนกันจน error
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Expense e WHERE e.id = :id")
    Optional<Expense> lockById(@Param("id") Long id);

    // แค่รหัสทริปของบิล (ไม่โหลดบิลเข้า persistence context ก่อน transaction
    // ที่ล็อกแถว
    // ไม่งั้น lockById จะได้ออบเจกต์เดิมที่ค้างอยู่
    // เห็นข้อมูลเก่าทั้งที่ล็อกได้แล้ว)
    @Query("SELECT e.trip.id FROM Expense e WHERE e.id = :id")
    Optional<Long> findTripIdById(@Param("id") Long id);

    // บิลที่บันทึกจากรายการในแพลน (ใช้ตอนลบรายการ)
    List<Expense> findByActivityId(Long activityId);

    // ล็อกบิลไว้จนจบ transaction: แก้/ลบบิลเดียวกันพร้อมกันหลายเครื่องจะต่อคิวกัน
    // ไม่ชนกันจน error
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Expense e WHERE e.id = :id")
    Optional<Expense> lockById(@Param("id") Long id);

    // แค่รหัสทริปของบิล (ไม่โหลดบิลเข้า persistence context ก่อน transaction
    // ที่ล็อกแถว
    // ไม่งั้น lockById จะได้ออบเจกต์เดิมที่ค้างอยู่
    // เห็นข้อมูลเก่าทั้งที่ล็อกได้แล้ว)
    @Query("SELECT e.trip.id FROM Expense e WHERE e.id = :id")
    Optional<Long> findTripIdById(@Param("id") Long id);

    // บิลที่บันทึกจากรายการในแพลน (ใช้ตอนลบรายการ)
    List<Expense> findByActivityId(Long activityId);

}