package com.cp.party_trip.service.impl;

import com.cp.party_trip.dto.response.MemberDebtSummaryResponse;
import com.cp.party_trip.service.DebtService;
import com.cp.party_trip.service.RepaymentService;
import com.cp.party_trip.model.DebtTransfer;
import com.cp.party_trip.model.Expense;
import com.cp.party_trip.model.ExpenseSplit;
import com.cp.party_trip.model.TripMember;
import com.cp.party_trip.model.Repayment;
import com.cp.party_trip.repository.ExpenseRepo;
import com.cp.party_trip.repository.ExpenseSplitRepo;
import com.cp.party_trip.repository.TripMemberRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class DebtServiceImpl implements DebtService {
    private final ExpenseRepo expenseRepo;
    private final ExpenseSplitRepo expenseSplitRepo;
    private final TripMemberRepo tripMemberRepo;
    private final RepaymentService repaymentService;

    public DebtServiceImpl(ExpenseRepo expenseRepo, ExpenseSplitRepo expenseSplitRepo, TripMemberRepo tripMemberRepo,
            RepaymentService repaymentService) {
        this.expenseRepo = expenseRepo;
        this.expenseSplitRepo = expenseSplitRepo;
        this.tripMemberRepo = tripMemberRepo;
        this.repaymentService = repaymentService;
    }

    // ใช้ TripMember เป็น key ของ Map ได้เพราะอยู่ใน transaction เดียวกัน (ได้ object ตัวเดิมเสมอ)
    // และโหลด expenseSplits แบบ lazy ได้โดยไม่ต้องพึ่ง open-in-view
    @Override
    @Transactional(readOnly = true)
    public List<DebtTransfer> calculateDebtSimplification(Long tripId) {
        List<Expense> expenses = expenseRepo.findByTripId(tripId);
        Map<TripMember, BigDecimal> balances = new HashMap<>();

        // คำนวณยอดเงินของสมาชิกทุกคน
        for (Expense expense : expenses) {
            TripMember paidBy = expense.getUser();
            if (paidBy == null || expense.getTotalAmount() == null)
                continue;
            balances.put(paidBy, balances.getOrDefault(paidBy, BigDecimal.ZERO).add(expense.getTotalAmount()));

            if (expense.getExpenseSplits() == null)
                continue;
            for (ExpenseSplit split : expense.getExpenseSplits()) {
                TripMember owedby = split.getTripMember();
                if (owedby == null || split.getAmountOwed() == null)
                    continue;
                // ส่วนที่จ่ายคืนแล้ว (ครบหรือบางส่วน) ไม่นับเป็นหนี้ เหลือแค่ยอดที่ยังค้าง
                BigDecimal paid = owedby.equals(paidBy) ? BigDecimal.ZERO : split.paidSoFar();
                if (paid.signum() > 0) {
                    balances.put(paidBy, balances.get(paidBy).subtract(paid));
                }
                balances.put(owedby, balances.getOrDefault(owedby, BigDecimal.ZERO)
                        .subtract(split.getAmountOwed().subtract(paid)));
            }
        }

        // แยกกลุ่มคนที่เป็นหนี้ (ต้องจ่ายเพิ่ม) และคนที่เป็นเจ้าหนี้ (ได้เงินคืน)
        List<Map.Entry<TripMember, BigDecimal>> debtors = new ArrayList<>();
        List<Map.Entry<TripMember, BigDecimal>> creditors = new ArrayList<>();

        for (Map.Entry<TripMember, BigDecimal> entry : balances.entrySet()) {
            int cmp = entry.getValue().compareTo(BigDecimal.ZERO);
            if (cmp < 0) {
                debtors.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().abs()));
            } else if (cmp > 0) {
                creditors.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()));
            }
        }

        // จับคู่หักล้างหนี้
        List<DebtTransfer> transfers = new ArrayList<>();
        int i = 0, j = 0;

        while (i < debtors.size() && j < creditors.size()) {
            Map.Entry<TripMember, BigDecimal> debtor = debtors.get(i);
            Map.Entry<TripMember, BigDecimal> creditor = creditors.get(j);

            BigDecimal debtAmount = debtor.getValue();
            BigDecimal creditAmount = creditor.getValue();

            BigDecimal minAmount = debtAmount.min(creditAmount).setScale(2, RoundingMode.HALF_UP);

            // ส่งยอดเงิน, ข้อมูลคนโอน, ข้อมูลคนรับ
            transfers.add(new DebtTransfer(minAmount, debtor.getKey(), creditor.getKey()));

            debtor.setValue(debtAmount.subtract(minAmount));
            creditor.setValue(creditAmount.subtract(minAmount));

            if (debtor.getValue().compareTo(BigDecimal.ZERO) == 0)
                i++;
            if (creditor.getValue().compareTo(BigDecimal.ZERO) == 0)
                j++;
        }

        return transfers;
    }

    // สรุปหนี้ของสมาชิกคนนี้ในทริป: myDebts (ต้องจ่ายใคร), myPaidBills (บิลที่สำรองจ่าย), repayments
    @Override
    @Transactional(readOnly = true)
    public MemberDebtSummaryResponse getMemberSummary(Long tripId, Long memberId) {
        // เจ้าหนี้ -> รายการที่ค้าง (คงลำดับตามบิลที่เจอก่อน)
        Map<Long, String> creditorNames = new LinkedHashMap<>();
        Map<Long, List<MemberDebtSummaryResponse.DebtItem>> debtItems = new LinkedHashMap<>();
        List<MemberDebtSummaryResponse.PaidBill> myPaidBills = new ArrayList<>();

        for (Expense expense : expenseRepo.findByTripId(tripId)) {
            TripMember payer = expense.getUser();
            if (payer == null || payer.getId() == null) {
                continue;
            }
            // ดึง splits จาก repo ตรง (ไม่พึ่ง lazy collection)
            List<ExpenseSplit> splits = expenseSplitRepo.findByExpenseId(expense.getId());
            if (splits == null) {
                continue;
            }
            String title = expense.getTitle() != null ? expense.getTitle() : "ไม่มีชื่อรายการ";

            if (payer.getId().equals(memberId)) {
                // บิลที่เราสำรองจ่าย: ส่วนของเพื่อนแต่ละคน
                List<MemberDebtSummaryResponse.PaidSplit> others = new ArrayList<>();
                for (ExpenseSplit split : splits) {
                    TripMember m = split.getTripMember();
                    if (m == null || m.getId() == null || m.getId().equals(memberId)) {
                        continue;
                    }
                    others.add(new MemberDebtSummaryResponse.PaidSplit(expense.getId(), m.getId(), nameOf(m),
                            orZero(split.getAmountOwed()), split.isPaid(), split.paidSoFar()));
                }
                myPaidBills.add(new MemberDebtSummaryResponse.PaidBill(expense.getId(), title,
                        orZero(expense.getTotalAmount()), others));
                continue;
            }

            // บิลที่คนอื่นจ่าย: ส่วนของเราที่ยังค้าง (หักที่จ่ายมาบางส่วนแล้ว)
            for (ExpenseSplit split : splits) {
                TripMember m = split.getTripMember();
                if (m == null || !memberId.equals(m.getId())) {
                    continue;
                }
                BigDecimal remaining = split.getAmountOwed() == null ? BigDecimal.ZERO : split.remainingAmount();
                if (remaining.signum() > 0) {
                    creditorNames.putIfAbsent(payer.getId(), nameOf(payer));
                    debtItems.computeIfAbsent(payer.getId(), k -> new ArrayList<>())
                            .add(new MemberDebtSummaryResponse.DebtItem(title, remaining, split.getAmountOwed(),
                                    split.paidSoFar()));
                }
            }
        }

        List<MemberDebtSummaryResponse.DebtGroup> myDebts = new ArrayList<>();
        debtItems.forEach((creditorId, items) -> myDebts.add(new MemberDebtSummaryResponse.DebtGroup(creditorId,
                creditorNames.get(creditorId),
                items.stream().map(MemberDebtSummaryResponse.DebtItem::amount).reduce(BigDecimal.ZERO,
                        BigDecimal::add),
                items)));
        return new MemberDebtSummaryResponse(myDebts, myPaidBills, repaymentsOf(tripId, memberId));
    }

    // รับเงินเป็นยอดรวมที่เกี่ยวกับเรา (เราได้รับ หรือเราโอนให้คนอื่น)
    private List<MemberDebtSummaryResponse.RepaymentView> repaymentsOf(Long tripId, Long memberId) {
        Map<Long, String> names = new HashMap<>();
        tripMemberRepo.findByTripId(tripId).forEach(m -> names.put(m.getId(), m.getGuestName()));
        List<MemberDebtSummaryResponse.RepaymentView> list = new ArrayList<>();
        for (Repayment r : repaymentService.tripRepayments(tripId)) {
            if (!memberId.equals(r.getFromMemberId()) && !memberId.equals(r.getToMemberId())) {
                continue;
            }
            List<MemberDebtSummaryResponse.RepaymentView.Item> items = r.getItems().stream()
                    .map(i -> new MemberDebtSummaryResponse.RepaymentView.Item(i.getExpenseId(), i.getTitle(),
                            i.getAmount()))
                    .toList();
            list.add(new MemberDebtSummaryResponse.RepaymentView(r.getId(), r.getFromMemberId(),
                    names.getOrDefault(r.getFromMemberId(), "สมาชิก #" + r.getFromMemberId()), r.getToMemberId(),
                    names.getOrDefault(r.getToMemberId(), "สมาชิก #" + r.getToMemberId()), r.getAmount(),
                    r.getCreatedAt(), items));
        }
        return list;
    }

    private static String nameOf(TripMember m) {
        return m.getGuestName() != null ? m.getGuestName() : "สมาชิก #" + m.getId();
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
