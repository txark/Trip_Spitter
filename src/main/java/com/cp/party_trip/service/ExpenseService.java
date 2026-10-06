package com.cp.party_trip.service;

import com.cp.party_trip.dto.response.ExpenseViewResponse;
import com.cp.party_trip.dto.request.ExpenseRequest;
import com.cp.party_trip.model.Expense;
import java.util.List;

// บิลค่าใช้จ่าย: สร้าง/แก้/ลบบิล แบ่งเงิน และยืนยันการรับเงิน
// ตัวจริงอยู่ที่ service/impl/ExpenseServiceImpl (Controller ขึ้นกับ interface นี้ ไม่ใช่คลาสจริง)
public interface ExpenseService {
    Expense createExpense(Long tripId, Long userId, ExpenseRequest request, List<Long> participantIds);

    // recordedBy = คนที่กดบันทึก (บันทึกแทนเพื่อนที่จ่ายได้), ไม่ส่ง = คนจ่ายบันทึกเอง
    Expense createExpense(Long tripId, Long userId, Long recordedBy, ExpenseRequest request,
            List<Long> participantIds);

    // แก้บิล: เฉพาะคนจ่าย และยังไม่มีเพื่อนคนไหนจ่ายคืน (ไม่งั้นยอดที่คืนไปแล้วจะไม่ตรงกับบิลใหม่)
    Expense updateExpense(Long expenseId, Long memberId, ExpenseRequest request, List<Long> participantIds);

    // newPayerId = เปลี่ยนคนจ่าย (ไม่ส่ง = คนเดิม) คนที่แก้จะกลายเป็นคนบันทึก จะได้ยังแก้บิลนี้ต่อได้
    Expense updateExpense(Long expenseId, Long memberId, Long newPayerId, ExpenseRequest request,
            List<Long> participantIds);

    // expectedRevision = รุ่นของบิลตอนเปิดฟอร์มแก้ (ไม่ตรงกับปัจจุบัน = อีกเครื่องแก้ไปแล้ว -> 409)
    Expense updateExpense(Long expenseId, Long memberId, Long newPayerId, ExpenseRequest request,
            List<Long> participantIds, Integer expectedRevision);

    void deleteExpense(Long expenseId, Long memberId);

    void deleteExpense(Long expenseId, Long memberId, Integer expectedRevision);

    List<Expense> getExpensesByTrip(Long tripId);

    // บิลทั้งทริป + การแบ่งเงินของแต่ละบิล ในรูปที่หน้าเว็บใช้
    List<ExpenseViewResponse> getTripExpenseViews(Long tripId);

    // คนจ่ายบิล (หรือคนบันทึกแทน) ยืนยันว่าได้รับเงินส่วนของ memberId ครบแล้ว
    // actingMemberId = สมาชิกของเจ้าของ token
    void markSplitPaid(Long expenseId, Long memberId, Long actingMemberId);
}
