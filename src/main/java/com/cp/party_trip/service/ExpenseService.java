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
import java.util.LinkedHashSet;
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ไม่พบข้อมูลทริป"));

        // ผู้จ่ายต้องเป็นสมาชิกของทริปนี้ (เดิมรับสมาชิกทริปอื่นได้)
        TripMember paidBy = findTripMember(tripId, userId);

        if (request.getTotalAmount() == null) {
            throw badRequest("ยอดบิลต้องมากกว่า 0");
        }
        // เก็บเงินเป็นทศนิยม 2 ตำแหน่งเสมอ ให้ยอดบิลกับผลรวมของแต่ละคนตรงกันพอดี
        BigDecimal totalAmount = request.getTotalAmount().setScale(2, RoundingMode.HALF_UP);
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw badRequest("ยอดบิลต้องมากกว่า 0");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw badRequest("กรุณาตั้งชื่อรายการ");
        }

        // ไม่ระบุวิธีหาร = หารเท่ากัน, ระบุแบบที่ไม่รองรับ = ผิด (เดิมบันทึกบิลโดยไม่มีผู้ร่วมหาร ทำให้ยอดหนี้เพี้ยน)
        String splitType = request.getSplitType() == null || request.getSplitType().isBlank()
                ? "EQUAL"
                : request.getSplitType().trim().toUpperCase();
        if (!"EQUAL".equals(splitType) && !"CUSTOM".equals(splitType)) {
            throw badRequest("วิธีหารไม่ถูกต้อง (รองรับ EQUAL หรือ CUSTOM)");
        }

        Expense expense = new Expense();
        expense.setTitle(request.getTitle().trim());
        expense.setTotalAmount(totalAmount);
        expense.setCurrency(request.getCurrency());
        expense.setCategory(request.getCategory());
        expense.setSplitType(splitType);
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
                if (s.getAmount().stripTrailingZeros().scale() > 2) {
                    throw badRequest("ยอดของแต่ละคนใส่ทศนิยมได้ไม่เกิน 2 ตำแหน่ง");
                }
                sum = sum.add(s.getAmount());
            }
            if (sum.setScale(2, RoundingMode.HALF_UP).compareTo(totalAmount) != 0) {
                throw badRequest("ผลรวมของแต่ละคน (" + sum + ") ไม่เท่ากับยอดบิล (" + totalAmount + ")");
            }

            for (ExpenseRequest.SplitAmount s : custom) {
                // คนที่ยอด 0 ไม่ต้องสร้าง split (ไม่ได้ร่วมจ่ายบิลนี้)
                if (s.getAmount().compareTo(BigDecimal.ZERO) == 0)
                    continue;
                splits.add(newSplit(expense, findTripMember(tripId, s.getMemberId()),
                        s.getAmount().setScale(2, RoundingMode.HALF_UP)));
            }
        } else {
            // EQUAL SPLIT: ตัดรายชื่อซ้ำออก (ซ้ำแล้วคนเดียวจะมี 2 split และกดจ่ายได้แค่อันเดียว)
            // ไม่มีผู้ร่วมหารเลย = ผู้จ่ายออกเองทั้งหมด
            List<Long> people = new ArrayList<>(new LinkedHashSet<>(
                    participantIds == null ? List.<Long>of() : participantIds));
            people.removeIf(java.util.Objects::isNull);
            if (people.isEmpty()) {
                people.add(paidBy.getId());
            }
            int count = people.size();
            BigDecimal perPerson = totalAmount.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
            // เศษจากการปัดทศนิยม (เช่น 100/3) ให้คนแรกรับไป เพื่อให้ผลรวม splits เท่ากับยอดบิลพอดี
            BigDecimal remainder = totalAmount.subtract(perPerson.multiply(BigDecimal.valueOf(count)));

            for (Long memberId : people) {
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
