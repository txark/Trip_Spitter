package com.cp.party_trip.service;

import com.cp.party_trip.dto.RepaymentRequest;
import com.cp.party_trip.model.Expense;
import com.cp.party_trip.model.ExpenseSplit;
import com.cp.party_trip.model.Repayment;
import com.cp.party_trip.model.RepaymentItem;
import com.cp.party_trip.model.TripMember;
import com.cp.party_trip.repository.ExpenseRepo;
import com.cp.party_trip.repository.ExpenseSplitRepo;
import com.cp.party_trip.repository.RepaymentRepo;
import com.cp.party_trip.repository.TripMemberRepo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

// เพื่อนโอนคืนเป็นยอดรวม: หักเคลียร์รายการที่ค้างเราตั้งแต่บิลเก่าสุด ยอดไม่พอ = รายการสุดท้ายจ่ายบางส่วน
@Service
public class RepaymentService {

    private final RepaymentRepo repaymentRepo;
    private final ExpenseRepo expenseRepo;
    private final ExpenseSplitRepo expenseSplitRepo;
    private final TripMemberRepo tripMemberRepo;

    public RepaymentService(RepaymentRepo repaymentRepo, ExpenseRepo expenseRepo, ExpenseSplitRepo expenseSplitRepo,
            TripMemberRepo tripMemberRepo) {
        this.repaymentRepo = repaymentRepo;
        this.expenseRepo = expenseRepo;
        this.expenseSplitRepo = expenseSplitRepo;
        this.tripMemberRepo = tripMemberRepo;
    }

    @Transactional
    public Repayment receive(Long tripId, Long receiverId, Long senderId, RepaymentRequest request) {
        TripMember receiver = member(tripId, receiverId);
        TripMember sender = member(tripId, senderId);
        // ต่อคิวการรับเงินจากผู้โอนคนนี้ (ยอดค้างต้องอ่านหลังจากรายการก่อนหน้าบันทึกเสร็จ)
        tripMemberRepo.lockById(sender.getId());
        if (receiver.getId().equals(sender.getId())) {
            throw badRequest("ผู้โอนกับผู้รับต้องเป็นคนละคนกัน");
        }
        BigDecimal amount = request == null || request.getAmount() == null ? null
                : request.getAmount().setScale(2, RoundingMode.HALF_UP);
        if (amount == null || amount.signum() <= 0) {
            throw badRequest("ยอดที่ได้รับต้องมากกว่า 0");
        }
        Set<Long> chosen = request.getExpenseIds() == null || request.getExpenseIds().isEmpty() ? null
                : new HashSet<>(request.getExpenseIds());

        // รายการที่ผู้โอนยังค้างผู้รับ เรียงบิลเก่าสุดก่อน
        List<Expense> bills = expenseRepo.findByTripId(tripId).stream()
                .filter(e -> e.getUser() != null && receiver.getId().equals(e.getUser().getId()))
                .filter(e -> chosen == null || chosen.contains(e.getId()))
                .sorted(Comparator.comparing(Expense::getId))
                .toList();
        List<ExpenseSplit> open = new ArrayList<>();
        Map<Long, Expense> billOf = bills.stream().collect(Collectors.toMap(Expense::getId, Function.identity()));
        for (Expense bill : bills) {
            for (ExpenseSplit split : expenseSplitRepo.findByExpenseId(bill.getId())) {
                if (split.getTripMember() != null && sender.getId().equals(split.getTripMember().getId())
                        && split.remainingAmount().signum() > 0) {
                    open.add(split);
                }
            }
        }
        BigDecimal outstanding = open.stream().map(ExpenseSplit::remainingAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (outstanding.signum() == 0) {
            throw badRequest(sender.getGuestName() + " ไม่มีรายการที่ค้างคุณ");
        }
        if (amount.compareTo(outstanding) > 0) {
            throw badRequest("ยอดที่ได้รับ (" + amount + ") มากกว่ายอดที่ค้างในรายการที่เลือก (" + outstanding + ")");
        }

        Repayment repayment = new Repayment();
        repayment.setTripId(tripId);
        repayment.setFromMemberId(sender.getId());
        repayment.setToMemberId(receiver.getId());
        repayment.setAmount(amount);
        BigDecimal left = amount;
        for (ExpenseSplit split : open) {
            if (left.signum() == 0) {
                break;
            }
            BigDecimal take = split.remainingAmount().min(left);
            applyPaid(split, split.paidSoFar().add(take));
            left = left.subtract(take);

            RepaymentItem item = new RepaymentItem();
            Expense bill = billOf.get(split.getExpense().getId());
            item.setExpenseId(bill.getId());
            item.setSplitId(split.getId());
            item.setTitle(bill.getTitle());
            item.setAmount(take);
            repayment.getItems().add(item);
        }
        expenseSplitRepo.saveAll(open);
        return repaymentRepo.save(repayment);
    }

    // ยกเลิก (เช่น กรอกยอดผิด): คืนยอดที่หักไปให้แต่ละรายการ เฉพาะคนที่รับเงิน
    @Transactional
    public void undo(Long repaymentId, Long memberId) {
        Repayment repayment = repaymentRepo.findById(repaymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ไม่พบรายการรับเงินนี้"));
        if (memberId == null || !memberId.equals(repayment.getToMemberId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ยกเลิกได้เฉพาะคนที่รับเงิน");
        }
        for (RepaymentItem item : repayment.getItems()) {
            if (item.getSplitId() == null) {
                continue;
            }
            expenseSplitRepo.findById(item.getSplitId()).ifPresent(split -> {
                BigDecimal back = split.paidSoFar().subtract(item.getAmount());
                applyPaid(split, back.max(BigDecimal.ZERO));
                expenseSplitRepo.save(split);
            });
        }
        repaymentRepo.delete(repayment);
    }

    public List<Repayment> tripRepayments(Long tripId) {
        return repaymentRepo.findByTripIdOrderByCreatedAtAsc(tripId);
    }

    // จ่ายครบ = ติ๊กจ่ายแล้ว, ยังไม่ครบ = เก็บยอดที่จ่ายมา
    static void applyPaid(ExpenseSplit split, BigDecimal paid) {
        BigDecimal owed = split.getAmountOwed() == null ? BigDecimal.ZERO : split.getAmountOwed();
        boolean full = paid.compareTo(owed) >= 0;
        split.setPaid(full);
        split.setPaidAmount(full ? owed : paid.signum() == 0 ? null : paid);
    }

    private TripMember member(Long tripId, Long memberId) {
        TripMember m = memberId == null ? null : tripMemberRepo.findById(memberId).orElse(null);
        if (m == null || m.getTrip() == null || !tripId.equals(m.getTrip().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ผู้โอนและผู้รับต้องเป็นสมาชิกในทริปนี้");
        }
        return m;
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
