package com.cp.party_trip.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.cp.party_trip.config.AuthGuard;
import com.cp.party_trip.dto.request.ExpenseRequest;
import com.cp.party_trip.dto.request.RepaymentRequest;
import com.cp.party_trip.dto.response.ExpenseResponse;
import com.cp.party_trip.dto.response.ExpenseViewResponse;
import com.cp.party_trip.dto.response.MessageResponse;
import com.cp.party_trip.dto.response.RepaymentResponse;
import com.cp.party_trip.mapper.DebtMapper;
import com.cp.party_trip.mapper.ExpenseMapper;
import com.cp.party_trip.service.ExpenseService;
import com.cp.party_trip.service.RepaymentService;
import jakarta.validation.Valid;
import com.cp.party_trip.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Expenses - บิลและการหาร")
@RestController
@RequestMapping("/api/v1")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final RepaymentService repaymentService;
    private final ExpenseMapper expenseMapper;
    private final DebtMapper debtMapper;
    private final AuthGuard guard;

    public ExpenseController(ExpenseService expenseService, RepaymentService repaymentService,
            ExpenseMapper expenseMapper, DebtMapper debtMapper, AuthGuard guard) {
        this.expenseService = expenseService;
        this.repaymentService = repaymentService;
        this.expenseMapper = expenseMapper;
        this.debtMapper = debtMapper;
        this.guard = guard;
    }

    // เพื่อนโอนคืนเป็นยอดรวม: receiverId = คนที่สำรองจ่าย (คนกด), senderId = คนที่โอนมา
    @PostMapping("/trips/{tripId}/repayments")
    public ResponseEntity<RepaymentResponse> receiveRepayment(@PathVariable Long tripId,
            @RequestParam Long receiverId, @RequestParam Long senderId, @Valid @RequestBody RepaymentRequest request) {
        guard.self(tripId, receiverId); // คนรับเงินเป็นคนบันทึก
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(debtMapper.toResponse(repaymentService.receive(tripId, receiverId, senderId, request)));
    }

    @DeleteMapping("/repayments/{repaymentId}")
    public ResponseEntity<Void> undoRepayment(@PathVariable Long repaymentId, @RequestParam Long memberId) {
        guard.self(guard.tripOfRepayment(repaymentId), memberId);
        repaymentService.undo(repaymentId, memberId);
        return ResponseEntity.noContent().build();
    }

    // คนบันทึก = เจ้าของ token (คนจ่ายเลือกเป็นเพื่อนได้)
    @PostMapping("/trips/{tripId}/expenses")
    public ResponseEntity<ExpenseResponse> addExpense(
            @PathVariable Long tripId,
            @RequestParam Long paidByMemberId,
            @Valid @RequestBody ExpenseRequest expense,
            @RequestParam(required = false) List<Long> participantIds,
            @RequestParam(required = false) Long recordedByMemberId) {
        Long recorder = guard.self(tripId, recordedByMemberId).getId();
        var saved = expenseService.createExpense(tripId, paidByMemberId, recorder, expense, participantIds);
        return ResponseEntity.status(HttpStatus.CREATED).body(expenseMapper.toResponse(saved));
    }

    // แก้บิล (memberId = คนที่กำลังแก้ ต้องเป็นคนจ่ายหรือคนบันทึก, paidByMemberId = เปลี่ยนคนจ่าย)
    @PutMapping("/expenses/{expenseId}")
    public ResponseEntity<ExpenseResponse> updateExpense(
            @PathVariable Long expenseId,
            @RequestParam Long memberId,
            @Valid @RequestBody ExpenseRequest expense,
            @RequestParam(required = false) List<Long> participantIds,
            @RequestParam(required = false) Long paidByMemberId,
            @RequestParam(required = false) Integer revision) {
        guard.self(guard.tripOfExpense(expenseId), memberId);
        return ResponseEntity.ok(expenseMapper.toResponse(expenseService.updateExpense(expenseId, memberId,
                paidByMemberId, expense, participantIds, revision)));
    }

    @DeleteMapping("/expenses/{expenseId}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long expenseId, @RequestParam Long memberId,
            @RequestParam(required = false) Integer revision) {
        guard.self(guard.tripOfExpense(expenseId), memberId);
        expenseService.deleteExpense(expenseId, memberId, revision);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/trips/{tripId}/expenses")
    public ResponseEntity<List<ExpenseViewResponse>> getExpensesByTrip(@PathVariable Long tripId) {
        guard.me(tripId);
        return ResponseEntity.ok(expenseService.getTripExpenseViews(tripId));
    }

    // แบ่งหน้า: ?page=0&size=10&sort=totalAmount,desc (ไม่ส่ง sort = ใหม่สุดก่อน, size สูงสุด 50)
    @GetMapping("/trips/{tripId}/expenses/page")
    public ResponseEntity<PageResponse<ExpenseViewResponse>> getExpensesByTripPage(@PathVariable Long tripId,
            @PageableDefault(size = 10) Pageable pageable) {
        guard.me(tripId);
        return ResponseEntity.ok(PageResponse.of(expenseService.getTripExpenseViewsPage(tripId, pageable),
                view -> view));
    }

    // ยืนยันรับเงินได้เฉพาะคนจ่ายบิล (หรือคนที่บันทึกบิลแทน) ตรวจใน service
    @PutMapping("/expenses/{expenseId}/splits/{memberId}/paid")
    public ResponseEntity<MessageResponse> markSplitAsPaid(@PathVariable Long expenseId,
            @PathVariable Long memberId) {
        Long me = guard.me(guard.tripOfExpense(expenseId)).getId();
        expenseService.markSplitPaid(expenseId, memberId, me);
        return ResponseEntity.ok(new MessageResponse("อัปเดตสถานะสำเร็จ"));
    }
}
