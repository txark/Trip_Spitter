package com.cp.party_trip.controller;

import com.cp.party_trip.config.AuthGuard;
import com.cp.party_trip.dto.ExpenseRequest;
import com.cp.party_trip.model.Expense;
import com.cp.party_trip.service.ExpenseService;
import com.cp.party_trip.service.RepaymentService;
import com.cp.party_trip.dto.RepaymentRequest;
import com.cp.party_trip.model.Repayment;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expenses")
@CrossOrigin(origins = { "http://127.0.0.1:5500", "http://localhost:5500" })
public class ExpenseController {

    private final ExpenseService expenseService;
    private final RepaymentService repaymentService;
    private final AuthGuard guard;

    public ExpenseController(ExpenseService expenseService, RepaymentService repaymentService,
            AuthGuard guard) {
        this.guard = guard;
        this.expenseService = expenseService;
        this.repaymentService = repaymentService;
    }

    // เพื่อนโอนคืนเป็นยอดรวม: receiverId = คนที่สำรองจ่าย (คนกด), senderId = คนที่โอนมา
    @PostMapping("/repay/{tripId}")
    public ResponseEntity<Repayment> receiveRepayment(@PathVariable Long tripId, @RequestParam Long receiverId,
            @RequestParam Long senderId, @RequestBody RepaymentRequest request) {
        guard.self(tripId, receiverId); // คนรับเงินเป็นคนบันทึก
        return ResponseEntity.ok(repaymentService.receive(tripId, receiverId, senderId, request));
    }

    @DeleteMapping("/repay/{repaymentId}")
    public ResponseEntity<Void> undoRepayment(@PathVariable Long repaymentId, @RequestParam Long memberId) {
        guard.self(guard.tripOfRepayment(repaymentId), memberId);
        repaymentService.undo(repaymentId, memberId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/add/{tripId}")
    public ResponseEntity<Expense> addExpense(
            @PathVariable Long tripId,
            @RequestParam Long paidByMemberId,
            @RequestBody ExpenseRequest expense,
            @RequestParam(required = false) List<Long> participantIds,
            @RequestParam(required = false) Long recordedByMemberId) {
        // คนบันทึก = เจ้าของ token (คนจ่ายเลือกเป็นเพื่อนได้)
        Long recorder = guard.self(tripId, recordedByMemberId).getId();
        Expense savedExpense = expenseService.createExpense(tripId, paidByMemberId, recorder, expense,
                participantIds);
        return ResponseEntity.ok(savedExpense);
    }

    // แก้บิล (memberId = คนที่กำลังแก้ ต้องเป็นคนจ่ายหรือคนบันทึก, paidByMemberId = เปลี่ยนคนจ่าย)
    @PutMapping("/{expenseId}")
    public ResponseEntity<Expense> updateExpense(
            @PathVariable Long expenseId,
            @RequestParam Long memberId,
            @RequestBody ExpenseRequest expense,
            @RequestParam(required = false) List<Long> participantIds,
            @RequestParam(required = false) Long paidByMemberId,
            @RequestParam(required = false) Integer revision) {
        guard.self(guard.tripOfExpense(expenseId), memberId);
        return ResponseEntity.ok(expenseService.updateExpense(expenseId, memberId, paidByMemberId, expense,
                participantIds, revision));
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long expenseId, @RequestParam Long memberId,
            @RequestParam(required = false) Integer revision) {
        guard.self(guard.tripOfExpense(expenseId), memberId);
        expenseService.deleteExpense(expenseId, memberId, revision);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/trip/{tripId}")
    public ResponseEntity<List<Map<String, Object>>> getExpensesByTrip(@PathVariable Long tripId) {
        guard.me(tripId);
        return ResponseEntity.ok(expenseService.getTripExpenseViews(tripId));
    }

    // ส่งข้อความไทยกลับเป็น {"message": ...} ทุกสถานะ (403/409 ของ Spring ไม่มี message ให้หน้าเว็บแสดง)
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> error(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode())
                .body(Map.of("message", e.getReason() == null ? "" : e.getReason()));
    }


    // ยืนยันรับเงินได้เฉพาะคนจ่ายบิล (หรือคนที่บันทึกบิลแทน) ตรวจใน service
    @PutMapping("/splits/{expenseId}/{memberId}/pay")
    public ResponseEntity<Map<String, Object>> markSplitAsPaid(@PathVariable Long expenseId,
            @PathVariable Long memberId) {
        Long me = guard.me(guard.tripOfExpense(expenseId)).getId();
        expenseService.markSplitPaid(expenseId, memberId, me);
        return ResponseEntity.ok(Map.of("message", "อัปเดตสถานะสำเร็จ"));
    }
}
