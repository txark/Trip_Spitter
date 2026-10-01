package com.cp.party_trip.service;

import com.cp.party_trip.dto.ExpenseRequest;
import com.cp.party_trip.model.Expense;
import com.cp.party_trip.model.ExpenseSplit;
import com.cp.party_trip.model.Trip;
import com.cp.party_trip.model.TripMember;
import com.cp.party_trip.repository.ExpenseRepo;
import com.cp.party_trip.repository.TripRepo;
import com.cp.party_trip.repository.TripMemberRepo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    public Expense createExpense(Long tripId, Long userId, ExpenseRequest request, List<Long> participantIds) {
        Trip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new RuntimeException("ไม่พบข้อมูลทริป"));

        TripMember paidBy = tripMemberRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("ไม่พบข้อมูลผู้จ่ายเงิน"));

        BigDecimal totalAmount = request.getTotalAmount();
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw badRequest("ยอดบิลต้องมากกว่า 0");
        }

        Expense expense = new Expense();
        expense.setTitle(request.getTitle());
        expense.setTotalAmount(totalAmount);
        expense.setCurrency(request.getCurrency());
        expense.setCategory(request.getCategory());
        expense.setSplitType(request.getSplitType() != null ? request.getSplitType().toUpperCase() : null);
        expense.setTrip(trip);
        expense.setUser(paidBy);

        List<ExpenseSplit> splits = new ArrayList<>();
        expense.setExpenseSplits(splits);

        if ("CUSTOM".equals(expense.getSplitType())) {
            // CUSTOM SPLIT: แต่ละคนจ่ายไม่เท่ากัน ตามยอดที่กรอกมา
            List<ExpenseRequest.SplitAmount> custom = request.getSplits();
            if (custom == null || custom.isEmpty()) {
                throw badRequest("กรุณาระบุยอดของผู้ร่วมหารแต่ละคน");
            }

            BigDecimal sum = BigDecimal.ZERO;
            Set<Long> seen = new HashSet<>();
            for (ExpenseRequest.SplitAmount s : custom) {
                if (s.getMemberId() == null || !seen.add(s.getMemberId())) {
                    throw badRequest("รายชื่อผู้ร่วมหารไม่ถูกต้อง");
                }
                if (s.getAmount() == null || s.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                    throw badRequest("ยอดของแต่ละคนต้องไม่ติดลบ");
                }
                sum = sum.add(s.getAmount());
            }
            if (sum.setScale(2, RoundingMode.HALF_UP).compareTo(totalAmount.setScale(2, RoundingMode.HALF_UP)) != 0) {
                throw badRequest("ผลรวมของแต่ละคน (" + sum + ") ไม่เท่ากับยอดบิล (" + totalAmount + ")");
            }

            for (ExpenseRequest.SplitAmount s : custom) {
                // คนที่ยอด 0 ไม่ต้องสร้าง split (ไม่ได้ร่วมจ่ายบิลนี้)
                if (s.getAmount().compareTo(BigDecimal.ZERO) == 0)
                    continue;
                splits.add(newSplit(expense, findTripMember(tripId, s.getMemberId()),
                        s.getAmount().setScale(2, RoundingMode.HALF_UP)));
            }
        } else if ("EQUAL".equals(expense.getSplitType()) && participantIds != null
                && !participantIds.isEmpty()) {
            // EQUAL SPLIT LOGIC Auto System
            int count = participantIds.size();
            BigDecimal perPerson = totalAmount.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
            // เศษจากการปัดทศนิยม (เช่น 100/3) ให้คนแรกรับไป เพื่อให้ผลรวม splits เท่ากับยอดบิลพอดี
            BigDecimal remainder = totalAmount.subtract(perPerson.multiply(BigDecimal.valueOf(count)));

            for (Long memberId : participantIds) {
                splits.add(newSplit(expense, findTripMember(tripId, memberId),
                        splits.isEmpty() ? perPerson.add(remainder) : perPerson));
            }
        }

        return expenseRepo.save(expense);
    }

    // หาสมาชิก และตรวจว่าอยู่ในทริปนี้จริง (กันส่ง ID สมาชิกของทริปอื่นมา)
    private TripMember findTripMember(Long tripId, Long memberId) {
        TripMember member = tripMemberRepo.findById(memberId)
                .orElseThrow(() -> badRequest("ไม่พบข้อมูลสมาชิกผู้ร่วมหาร"));
        if (member.getTrip() == null || !tripId.equals(member.getTrip().getId())) {
            throw badRequest("สมาชิก #" + memberId + " ไม่ได้อยู่ในทริปนี้");
        }
        return member;
    }

    private ExpenseSplit newSplit(Expense expense, TripMember member, BigDecimal amount) {
        ExpenseSplit split = new ExpenseSplit();
        split.setExpense(expense);
        split.setTripMember(member);
        split.setAmountOwed(amount);
        return split;
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    public List<Expense> getExpensesByTrip(Long tripId) {
        return expenseRepo.findByTripId(tripId);
    }
}
