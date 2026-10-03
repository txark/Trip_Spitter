package com.cp.party_trip.controller;

import com.cp.party_trip.config.AuthGuard;
import com.cp.party_trip.dto.ExpenseRequest;
import com.cp.party_trip.model.Expense;
import com.cp.party_trip.model.ExpenseSplit;
import com.cp.party_trip.service.ExpenseService;
import com.cp.party_trip.service.RepaymentService;
import com.cp.party_trip.dto.RepaymentRequest;
import com.cp.party_trip.model.Repayment;
import com.cp.party_trip.repository.ExpenseRepo;
import com.cp.party_trip.repository.ExpenseSplitRepo;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expenses")
@CrossOrigin(origins = { "http://127.0.0.1:5500", "http://localhost:5500" })
public class ExpenseController {

    private final ExpenseService expenseService;
    private final ExpenseRepo expenseRepo;
    private final ExpenseSplitRepo expenseSplitRepo;
    private final RepaymentService repaymentService;
    private final AuthGuard guard;

    public ExpenseController(ExpenseService expenseService, ExpenseRepo expenseRepo,
            ExpenseSplitRepo expenseSplitRepo, RepaymentService repaymentService, AuthGuard guard) {
        this.guard = guard;
        this.expenseService = expenseService;
        this.expenseRepo = expenseRepo;
        this.expenseSplitRepo = expenseSplitRepo;
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
            @RequestParam(required = false) Long paidByMemberId) {
        guard.self(guard.tripOfExpense(expenseId), memberId);
        return ResponseEntity.ok(expenseService.updateExpense(expenseId, memberId, paidByMemberId, expense,
                participantIds));
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long expenseId, @RequestParam Long memberId) {
        guard.self(guard.tripOfExpense(expenseId), memberId);
        expenseService.deleteExpense(expenseId, memberId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/trip/{tripId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getExpensesByTrip(@PathVariable Long tripId) {
        List<Expense> expenses = expenseRepo.findByTripId(tripId);
        List<Map<String, Object>> result = new ArrayList<>();

        if (expenses != null) {
            for (Expense exp : expenses) {
                Map<String, Object> expMap = new HashMap<>();
                expMap.put("id", exp.getId());
                expMap.put("title", exp.getTitle());
                expMap.put("totalAmount", exp.getTotalAmount());
                expMap.put("category", exp.getCategory());
                expMap.put("splitType", exp.getSplitType());
                expMap.put("expenseDate", exp.getExpenseDate());
                expMap.put("activityId", exp.getActivityId());
                expMap.put("recordedById", exp.getRecordedById());
                expMap.put("currency", exp.getCurrency());
                expMap.put("originalAmount", exp.getOriginalAmount());
                expMap.put("exchangeRate", exp.getExchangeRate());

                if (exp.getUser() != null) {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", exp.getUser().getId());
                    userMap.put("guestName", exp.getUser().getGuestName());
                    expMap.put("user", userMap);
                }

                List<ExpenseSplit> splits = expenseSplitRepo.findByExpenseId(exp.getId());
                List<Map<String, Object>> splitsList = new ArrayList<>();

                if (splits != null) {
                    for (ExpenseSplit split : splits) {
                        Map<String, Object> splitMap = new HashMap<>();
                        splitMap.put("id", split.getId());
                        splitMap.put("amountOwed", split.getAmountOwed());

                        boolean isPaidStatus = false;
                        try {
                            isPaidStatus = split.isPaid();
                        } catch (Exception e) {
                            isPaidStatus = false;
                        }
                        splitMap.put("isPaid", isPaidStatus);
                        splitMap.put("paidAmount", split.paidSoFar());

                        if (split.getTripMember() != null) {
                            Map<String, Object> tmMap = new HashMap<>();
                            tmMap.put("id", split.getTripMember().getId());
                            tmMap.put("guestName", split.getTripMember().getGuestName());
                            splitMap.put("tripMember", tmMap);
                        }
                        splitsList.add(splitMap);
                    }
                }

                expMap.put("expenseSplits", splitsList);
                expMap.put("splits", splitsList);

                result.add(expMap);
            }
        }
        return ResponseEntity.ok(result);
    }

    // ส่งข้อความไทยกลับเป็น {"message": ...} ทุกสถานะ (403/409 ของ Spring ไม่มี message ให้หน้าเว็บแสดง)
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> error(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode())
                .body(Map.of("message", e.getReason() == null ? "" : e.getReason()));
    }

    @PutMapping("/splits/{expenseId}/{memberId}/pay")
    public ResponseEntity<?> markSplitAsPaid(@PathVariable Long expenseId, @PathVariable Long memberId) {
        // ยืนยันรับเงินได้เฉพาะคนจ่ายบิล (หรือคนที่บันทึกบิลแทน)
        Long me = guard.me(guard.tripOfExpense(expenseId)).getId();
        Expense expense = expenseRepo.findById(expenseId).orElseThrow();
        boolean owner = (expense.getUser() != null && me.equals(expense.getUser().getId()))
                || me.equals(expense.getRecordedById());
        if (!owner) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "ยืนยันรับเงินได้เฉพาะคนที่จ่ายบิลนี้");
        }
        // คนบันทึกแทนที่เป็นลูกหนี้ในบิลเดียวกัน ห้ามกดว่าตัวเองจ่ายแล้ว (ให้คนจ่ายจริงยืนยัน)
        boolean payer = expense.getUser() != null && me.equals(expense.getUser().getId());
        if (me.equals(memberId) && !payer) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "ยืนยันว่าตัวเองจ่ายคืนแล้วไม่ได้ ให้คนที่จ่ายบิลเป็นคนยืนยัน");
        }
        List<ExpenseSplit> splits = expenseSplitRepo.findByExpenseId(expenseId);

        if (splits != null) {
            for (ExpenseSplit split : splits) {
                if (split.getTripMember() != null && split.getTripMember().getId().equals(memberId)) {
                    split.setPaid(true); // ได้รับส่วนที่เหลือครบแล้ว
                    split.setPaidAmount(split.getAmountOwed());
                    expenseSplitRepo.save(split);
                    return ResponseEntity.ok().body(Map.of("message", "อัปเดตสถานะสำเร็จ"));
                }
            }
        }
        return ResponseEntity.notFound().build();
    }
}