package com.cp.party_trip.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.cp.party_trip.model.TripMember;
import java.util.List;
import java.util.Optional;

public interface TripMemberRepo extends JpaRepository<TripMember, Long> {
    List<TripMember> findByTripId(Long tripId);

    // ล็อกสมาชิกคนนี้ไว้จนจบ transaction (รับเงินก้อนจากคนเดียวกันพร้อมกัน ต้องต่อคิว ไม่งั้นหักเกินยอดค้าง)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM TripMember m WHERE m.id = :id")
    Optional<TripMember> lockById(@Param("id") Long id);

    // ใช้ First กันข้อมูลเก่าที่มีชื่อซ้ำในทริปเดียวกัน (Optional ธรรมดาจะ error เมื่อเจอหลายแถว)
    Optional<TripMember> findFirstByTripIdAndGuestNameOrderByIdAsc(Long tripId, String guestName);

    // เปลี่ยนชื่อเล่น: ทริปที่มีทั้งชื่อเดิม (เรา) และชื่อใหม่ (คนอื่น) อยู่แล้ว = ชื่อชนกัน
    @Query("SELECT COUNT(m) FROM TripMember m WHERE m.guestName = :newName AND m.trip.id IN "
            + "(SELECT o.trip.id FROM TripMember o WHERE o.guestName = :oldName)")
    long countNameClashes(@Param("oldName") String oldName, @Param("newName") String newName);

    // สมาชิกผูกกับผู้ใช้ด้วยชื่อเล่น: เปลี่ยนชื่อแล้วต้องย้ายชื่อในทุกทริปตามไปด้วย
    @Modifying
    @Query("UPDATE TripMember m SET m.guestName = :newName WHERE m.guestName = :oldName")
    int renameGuest(@Param("oldName") String oldName, @Param("newName") String newName);
}