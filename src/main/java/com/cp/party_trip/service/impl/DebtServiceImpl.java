package com.cp.party_trip.service.impl;

import com.cp.party_trip.service.DebtService;
import com.cp.party_trip.service.RepaymentService;
import com.cp.party_trip.dto.DebtTransfer;
import com.cp.party_trip.model.Expense;
import com.cp.party_trip.model.ExpenseSplit;
import com.cp.party_trip.model.TripMember;
import com.cp.party_trip.model.Repayment;
import com.cp.party_trip.model.RepaymentItem;
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
    public Map<String, Object> getMemberSummary(Long tripId, Long memberId) {
        List<Expense> expenses = expenseRepo.findByTripId(tripId);
        Map<String, Object> response = new HashMap<>();

        List<Map<String, Object>> myDebts = new ArrayList<>();
        List<Map<String, Object>> myPaidBills = new ArrayList<>();

        response.put("repayments", repaymentsOf(tripId, memberId));
        if (expenses == null || expenses.isEmpty()) {
            response.put("myDebts", myDebts);
            response.put("myPaidBills", myPaidBills);
            return response;
        }

        // memberId ที่ส่งมาคือ TripMember ID ของผู้ใช้ในทริปนี้
        Long targetTripMemberId = memberId;

        for (Expense expense : expenses) {
            TripMember payer = expense.getUser();
            if (payer == null || payer.getId() == null)
                continue;

            boolean isIPaid = payer.getId().equals(targetTripMemberId);

            // ดึง splits จาก repo ตรง (ไม่พึ่ง lazy collection)
            List<ExpenseSplit> splits = expenseSplitRepo.findByExpenseId(expense.getId());
            if (splits == null)
                continue;

            if (isIPaid) {
                // --- หมวด 2: บิลที่เราสำรองจ่าย ---
                Map<String, Object> paidBill = new HashMap<>();
                paidBill.put("expenseId", expense.getId());
                paidBill.put("title", expense.getTitle() != null ? expense.getTitle() : "ไม่มีชื่อรายการ");
                paidBill.put("totalAmount",
                        expense.getTotalAmount() != null ? expense.getTotalAmount() : BigDecimal.ZERO);

                List<Map<String, Object>> splitsInfo = new ArrayList<>();
                for (ExpenseSplit split : splits) {
                    if (split.getTripMember() != null && split.getTripMember().getId() != null
                            && !split.getTripMember().getId().equals(targetTripMemberId)) {
                        Map<String, Object> sInfo = new HashMap<>();
                        sInfo.put("expenseId", expense.getId());
                        sInfo.put("memberId", split.getTripMember().getId());
                        sInfo.put("memberName",
                                split.getTripMember().getGuestName() != null ? split.getTripMember().getGuestName()
                                        : "สมาชิก #" + split.getTripMember().getId());
                        sInfo.put("amountOwed",
                                split.getAmountOwed() != null ? split.getAmountOwed() : BigDecimal.ZERO);

                        boolean paidStatus = false;
                        try {
                            paidStatus = split.isPaid();
                        } catch (Exception e) {
                            paidStatus = false;
                        }
                        sInfo.put("isPaid", paidStatus);
                        sInfo.put("paidAmount", split.paidSoFar());

                        splitsInfo.add(sInfo);
                    }
                }
                paidBill.put("splits", splitsInfo);
                myPaidBills.add(paidBill);
            } else {
                // --- หมวด 1: บิลที่คนอื่นจ่าย (เราต้องร่วมหาร) ---
                for (ExpenseSplit split : splits) {
                    if (split.getTripMember() != null && split.getTripMember().getId() != null
                            && split.getTripMember().getId().equals(targetTripMemberId)) {
                        // ยอดที่ยังค้าง (หักส่วนที่จ่ายมาบางส่วนแล้ว)
                        BigDecimal remaining = split.getAmountOwed() == null ? BigDecimal.ZERO : split.remainingAmount();
                        if (remaining.compareTo(BigDecimal.ZERO) > 0) {

                            String creditorName = payer.getGuestName() != null ? payer.getGuestName()
                                    : "สมาชิก #" + payer.getId();

                            Map<String, Object> targetCreditor = null;
                            for (Map<String, Object> debtGroup : myDebts) {
                                if (payer.getId().equals(debtGroup.get("creditorId"))) {
                                    targetCreditor = debtGroup;
                                    break;
                                }
                            }

                            if (targetCreditor == null) {
                                targetCreditor = new HashMap<>();
                                targetCreditor.put("creditorId", payer.getId());
                                targetCreditor.put("creditorName", creditorName);
                                targetCreditor.put("totalAmount", BigDecimal.ZERO);
                                targetCreditor.put("items", new ArrayList<Map<String, Object>>());
                                myDebts.add(targetCreditor);
                            }

                            BigDecimal currentTotal = (BigDecimal) targetCreditor.get("totalAmount");
                            targetCreditor.put("totalAmount", currentTotal.add(remaining));

                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> items = (List<Map<String, Object>>) targetCreditor.get("items");
                            Map<String, Object> item = new HashMap<>();
                            item.put("title", expense.getTitle() != null ? expense.getTitle() : "ไม่มีชื่อรายการ");
                            item.put("amount", remaining);
                            item.put("owed", split.getAmountOwed());
                            item.put("paid", split.paidSoFar());
                            items.add(item);
                        }
                    }
                }
            }
        }

        response.put("myDebts", myDebts);
        response.put("myPaidBills", myPaidBills);
        return response;
    }

    // รับเงินเป็นยอดรวมที่เกี่ยวกับเรา (เราได้รับ หรือเราโอนให้คนอื่น)
    private List<Map<String, Object>> repaymentsOf(Long tripId, Long memberId) {
        Map<Long, String> names = new HashMap<>();
        tripMemberRepo.findByTripId(tripId).forEach(m -> names.put(m.getId(), m.getGuestName()));
        List<Map<String, Object>> list = new ArrayList<>();
        for (Repayment r : repaymentService.tripRepayments(tripId)) {
            if (!memberId.equals(r.getFromMemberId()) && !memberId.equals(r.getToMemberId())) {
                continue;
            }
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("fromMemberId", r.getFromMemberId());
            map.put("fromName", names.getOrDefault(r.getFromMemberId(), "สมาชิก #" + r.getFromMemberId()));
            map.put("toMemberId", r.getToMemberId());
            map.put("toName", names.getOrDefault(r.getToMemberId(), "สมาชิก #" + r.getToMemberId()));
            map.put("amount", r.getAmount());
            map.put("createdAt", r.getCreatedAt());
            List<Map<String, Object>> items = new ArrayList<>();
            for (RepaymentItem item : r.getItems()) {
                Map<String, Object> i = new HashMap<>();
                i.put("expenseId", item.getExpenseId());
                i.put("title", item.getTitle());
                i.put("amount", item.getAmount());
                items.add(i);
            }
            map.put("items", items);
            list.add(map);
        }
        return list;
    }
}
