package com.cp.party_trip.controller;

import com.cp.party_trip.dto.request.ExpenseRequest;
import com.cp.party_trip.dto.response.ExpenseResponse;
import com.cp.party_trip.model.Expense;
import com.cp.party_trip.model.ExpenseSplit;
import com.cp.party_trip.service.ExpenseService;
import com.cp.party_trip.repository.ExpenseRepo;
import com.cp.party_trip.repository.ExpenseSplitRepo;

import org.springframework.http.HttpStatus;
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
public class ExpenseController {

    private final ExpenseService expenseService;
    private final ExpenseRepo expenseRepo;
    private final ExpenseSplitRepo expenseSplitRepo;

    public ExpenseController(ExpenseService expenseService, ExpenseRepo expenseRepo,
            ExpenseSplitRepo expenseSplitRepo) {
        this.expenseService = expenseService;
        this.expenseRepo = expenseRepo;
        this.expenseSplitRepo = expenseSplitRepo;
    }

    private void guardSelf(Object trip, Long memberId) {
        if (memberId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Member access is required");
        }
    }

    private Object tripOfExpense(Long expenseId) {
        if (!expenseRepo.existsById(expenseId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Expense not found");
        }
        return expenseId;
    }

    // คนบันทึก = เจ้าของ token (คนจ่ายเลือกเป็นเพื่อนได้)
    @PostMapping("/add/{tripId}")
    public ResponseEntity<Expense> addExpense(
            @PathVariable Long tripId,
            @RequestParam Long paidByMemberId,
            @RequestBody ExpenseRequest expense,
            @RequestParam(required = false) List<Long> participantIds) {
        Expense savedExpense = expenseService.createExpense(tripId, paidByMemberId, expense, participantIds);
        return ResponseEntity.ok(savedExpense);
    }

    // แก้บิล (memberId = คนที่กำลังแก้ ต้องเป็นคนจ่ายหรือคนบันทึก, paidByMemberId =
    // เปลี่ยนคนจ่าย)
    @PutMapping("/{expenseId}")
    public ResponseEntity<Expense> updateExpense(
            @PathVariable Long expenseId,
            @RequestParam Long memberId,
            @RequestBody ExpenseRequest expense,
            @RequestParam(required = false) List<Long> participantIds,
            @RequestParam(required = false) Long paidByMemberId,
            @RequestParam(required = false) Integer revision) {
        guardSelf(tripOfExpense(expenseId), memberId);

        return ResponseEntity.ok(expenseService.updateExpense(expenseId, expense, memberId,
                participantIds, paidByMemberId, revision));
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long expenseId, @RequestParam Long memberId) {
        guardSelf(tripOfExpense(expenseId), memberId);
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

    @PutMapping("/splits/{expenseId}/{memberId}/pay")
    public ResponseEntity<?> markSplitAsPaid(@PathVariable Long expenseId, @PathVariable Long memberId) {
        List<ExpenseSplit> splits = expenseSplitRepo.findByExpenseId(expenseId);

        if (splits != null) {
            for (ExpenseSplit split : splits) {
                if (split.getTripMember() != null && split.getTripMember().getId().equals(memberId)) {
                    split.setPaid(true);
                    expenseSplitRepo.save(split);
                    return ResponseEntity.ok().body(Map.of("message", "อัปเดตสถานะสำเร็จ"));
                }
            }
        }
        return ResponseEntity.notFound().build();
    }
}