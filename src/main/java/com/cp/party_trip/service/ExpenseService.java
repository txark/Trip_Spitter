package com.cp.party_trip.service;

import com.cp.party_trip.model.Expense;
import com.cp.party_trip.model.ExpenseSplit;
import com.cp.party_trip.model.Trip;
import com.cp.party_trip.model.TripMember;
import com.cp.party_trip.repository.ExpenseRepo;
import com.cp.party_trip.repository.TripRepo;
import com.cp.party_trip.repository.TripMemberRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.ArrayList;

@Service
public class ExpenseService {

    private final ExpenseRepo expenseRepo;
    private final TripRepo tripRepo;
    private final TripMemberRepo tripMemberRepo;

    public ExpenseService(ExpenseRepo expenseRepo, TripRepo tripRepo, TripMemberRepo tripMemberRepo) {
        this.expenseRepo = expenseRepo;
        this.tripRepo = tripRepo;
        this.tripMemberRepo = tripMemberRepo;
    }

    @Transactional
    public Expense createExpense(Long tripId, Long userId, Expense expenseRequest, List<Long> participantIds) {
        Trip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new RuntimeException("ไม่พบข้อมูลทริป"));

        TripMember paidBy = tripMemberRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("ไม่พบข้อมูลผู้จ่ายเงิน"));

        expenseRequest.setTrip(trip);
        expenseRequest.setUser(paidBy);

        // EQUAL SPLIT LOGIC Auto System
        if ("EQUAL".equalsIgnoreCase(expenseRequest.getSplitType()) && participantIds != null
                && !participantIds.isEmpty()) {
            BigDecimal totalAmount = expenseRequest.getTotalAmount();
            int count = participantIds.size();
            BigDecimal perPerson = totalAmount.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);

            List<ExpenseSplit> splits = new ArrayList<>();
            expenseRequest.setExpenseSplits(splits);

            Expense savedExpense = expenseRepo.save(expenseRequest);

            for (Long memberId : participantIds) {
                TripMember member = tripMemberRepo.findById(memberId)
                        .orElseThrow(() -> new RuntimeException("ไม่พบข้อมูลสมาชิกผู้ร่วมหาร"));

                ExpenseSplit split = new ExpenseSplit();
                split.setExpense(savedExpense);
                split.setTripMember(member);
                split.setAmountOwed(perPerson);
                splits.add(split);
            }
            return expenseRepo.save(savedExpense);
        }

        return expenseRepo.save(expenseRequest);
    }

    public List<Expense> getExpensesByTrip(Long tripId) {
        return expenseRepo.findByTripId(tripId);
    }
}
