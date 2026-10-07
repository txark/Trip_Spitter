package com.cp.party_trip.service;

import com.cp.party_trip.dto.response.MemberDebtSummaryResponse;
import com.cp.party_trip.model.DebtTransfer;

import java.util.List;

// สรุปหนี้: ใครต้องโอนให้ใคร และสรุปหนี้ของสมาชิกแต่ละคน
// ตัวจริงอยู่ที่ service/impl/DebtServiceImpl (Controller ขึ้นกับ interface นี้ ไม่ใช่คลาสจริง)
public interface DebtService {
    // ใครต้องโอนให้ใครเท่าไหร่ (รวบยอดให้จำนวนครั้งที่โอนน้อยที่สุด หักยอดที่จ่ายคืนแล้ว)
    List<DebtTransfer> calculateDebtSimplification(Long tripId);

    // สรุปหนี้ของสมาชิกคนนี้ในทริป: myDebts (ต้องจ่ายใคร), myPaidBills (บิลที่สำรองจ่าย), repayments
    MemberDebtSummaryResponse getMemberSummary(Long tripId, Long memberId);
}
