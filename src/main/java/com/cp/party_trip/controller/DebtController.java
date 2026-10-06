package com.cp.party_trip.controller;

import com.cp.party_trip.dto.DebtTransfer;
import com.cp.party_trip.model.Expense;
import com.cp.party_trip.model.ExpenseSplit;
import com.cp.party_trip.model.TripMember;
import com.cp.party_trip.repository.ExpenseRepo;
import com.cp.party_trip.repository.ExpenseSplitRepo;
import com.cp.party_trip.service.DebtService;
import com.cp.party_trip.service.RepaymentService;
import com.cp.party_trip.model.Repayment;
import com.cp.party_trip.model.RepaymentItem;
import com.cp.party_trip.repository.TripMemberRepo;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/debts")
@CrossOrigin(origins = { "http://127.0.0.1:5500", "http://localhost:5500" })
public class DebtController {

    private final DebtService debtService;
    private final ExpenseRepo expenseRepo;
    private final ExpenseSplitRepo expenseSplitRepo; // 👈 เพิ่มตัวนี้เพื่อดึงข้อมูลตรง ป้องกันบัค 500

    private final RepaymentService repaymentService;
    private final TripMemberRepo tripMemberRepo;

    public DebtController(DebtService debtService, ExpenseRepo expenseRepo, ExpenseSplitRepo expenseSplitRepo,
            RepaymentService repaymentService, TripMemberRepo tripMemberRepo) {
        this.debtService = debtService;
        this.expenseRepo = expenseRepo;
        this.expenseSplitRepo = expenseSplitRepo;
        this.repaymentService = repaymentService;
        this.tripMemberRepo = tripMemberRepo;
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

    @GetMapping("/simplify/{tripId}")
    public ResponseEntity<List<DebtTransfer>> getSimplifiedDebts(@PathVariable Long tripId) {
        List<DebtTransfer> transfers = debtService.calculateDebtSimplification(tripId);
        return ResponseEntity.ok(transfers);
    }

    @GetMapping("/summary-details/{tripId}")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getTripDebtSummary(@PathVariable Long tripId,
            @RequestParam Long userId) {
        List<Expense> expenses = expenseRepo.findByTripId(tripId);
        Map<String, Object> response = new HashMap<>();

        List<Map<String, Object>> myDebts = new ArrayList<>();
        List<Map<String, Object>> myPaidBills = new ArrayList<>();

        response.put("repayments", repaymentsOf(tripId, userId));
        if (expenses == null || expenses.isEmpty()) {
            response.put("myDebts", myDebts);
            response.put("myPaidBills", myPaidBills);
            return ResponseEntity.ok(response);
        }

        // userId ที่ส่งมาคือ TripMember ID ของผู้ใช้ในทริปนี้
        Long targetTripMemberId = userId;

        for (Expense expense : expenses) {
            TripMember payer = expense.getUser();
            if (payer == null || payer.getId() == null)
                continue;

            boolean isIPaid = payer.getId().equals(targetTripMemberId);

            // 👈 ดึงข้อมูล Splits ด้วย Repo โดยตรง (ทางแก้บัค 500)
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
                for (ExpenseSplit split : splits) { // 👈 ใช้ splits จาก Repo
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
                for (ExpenseSplit split : splits) { // 👈 ใช้ splits จาก Repo
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
        return ResponseEntity.ok(response);
    }
}