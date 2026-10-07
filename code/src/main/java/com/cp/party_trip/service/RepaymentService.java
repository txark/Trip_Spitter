package com.cp.party_trip.service;

import com.cp.party_trip.dto.request.RepaymentRequest;
import com.cp.party_trip.model.Repayment;
import java.util.List;

// รับเงินคืนเป็นยอดรวม (หักบิลที่ค้างจากเก่าไปใหม่) และยกเลิก
// ตัวจริงอยู่ที่ service/impl/RepaymentServiceImpl (Controller ขึ้นกับ interface นี้ ไม่ใช่คลาสจริง)
public interface RepaymentService {
    Repayment receive(Long tripId, Long receiverId, Long senderId, RepaymentRequest request);

    // ยกเลิก (เช่น กรอกยอดผิด): คืนยอดที่หักไปให้แต่ละรายการ เฉพาะคนที่รับเงิน
    void undo(Long repaymentId, Long memberId);

    List<Repayment> tripRepayments(Long tripId);
}
