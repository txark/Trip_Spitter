package com.cp.party_trip.repository;

import com.cp.party_trip.model.Repayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RepaymentRepo extends JpaRepository<Repayment, Long> {
    List<Repayment> findByTripIdOrderByCreatedAtAsc(Long tripId);

    // รายการรับเงินล่าสุดของคู่ผู้โอน-ผู้รับ (ใช้ตรวจว่ายกเลิกได้เฉพาะรายการล่าสุด)
    Optional<Repayment> findFirstByTripIdAndFromMemberIdAndToMemberIdOrderByIdDesc(Long tripId, Long fromMemberId,
            Long toMemberId);
}
