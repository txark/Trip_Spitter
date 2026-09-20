package com.cp.party_trip.controller;

import com.cp.party_trip.model.Expense;
import com.cp.party_trip.service.ExpenseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping("/add/{tripId}")
    public ResponseEntity<Expense> addExpense(
            @PathVariable Long tripId,
            @RequestParam Long paidByMemberId,
            @RequestBody Expense expense,
            @RequestParam List<Long> participantIds) {
        Expense savedExpense = expenseService.createExpense(tripId, paidByMemberId, expense, participantIds);
        return ResponseEntity.ok(savedExpense);
    }

    @GetMapping("/trip/{tripId}")
    public ResponseEntity<List<Expense>> getExpensesByTrip(@PathVariable Long tripId) {
        List<Expense> expenses = expenseService.getExpensesByTrip(tripId);
        return ResponseEntity.ok(expenses);
    }
}