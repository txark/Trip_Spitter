package com.cp.party_trip.service;

import com.cp.party_trip.model.TripMember;

// ข้อมูลที่ AuthGuard ต้องใช้ตรวจสิทธิ์: "ผู้ใช้นี้เป็นสมาชิกทริปไหม" และ "ข้อมูลชิ้นนี้อยู่ทริปไหน"
// แยกเป็น interface เล็กของตัวเอง (ISP) เพื่อให้ชั้น config เรียกผ่าน Service ไม่ต้องแตะ Repository
public interface TripAccessService {
    // สมาชิกของผู้ใช้ในทริปนั้น (ไม่ใช่สมาชิก = 403)
    TripMember requireMember(Long tripId, String username);

    // รหัสทริปของข้อมูลชิ้นนั้น (ไม่พบ = 404)
    Long tripOfActivity(Long activityId);

    Long tripOfExpense(Long expenseId);

    Long tripOfPoll(Long pollId);

    Long tripOfChecklistItem(Long itemId);

    Long tripOfRepayment(Long repaymentId);
}
