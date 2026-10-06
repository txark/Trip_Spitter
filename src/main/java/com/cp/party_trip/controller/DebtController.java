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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/debts")
public class DebtController {

    private final DebtService debtService;
    private final DebtMapper debtMapper;
    private final AuthGuard guard;

    public DebtController(DebtService debtService, ExpenseRepo expenseRepo, ExpenseSplitRepo expenseSplitRepo) {
        this.debtService = debtService;
        this.expenseRepo = expenseRepo;
        this.expenseSplitRepo = expenseSplitRepo;
    }

    // ใครต้องโอนให้ใคร (รวบยอดแล้ว)
    @GetMapping("/simplify/{tripId}")
    public ResponseEntity<List<DebtTransfer>> getSimplifiedDebts(@PathVariable Long tripId) {
        List<DebtTransfer> transfers = debtService.calculateDebtSimplification(tripId);
        return ResponseEntity.ok(transfers);
    }

    @GetMapping("/summary-details/{tripId}")
    public ResponseEntity<MemberDebtSummaryResponse> getTripDebtSummary(@PathVariable Long tripId,
            @RequestParam Long userId) {
        List<Expense> expenses = expenseRepo.findByTripId(tripId);
        Map<String, Object> response = new HashMap<>();

        List<Map<String, Object>> myDebts = new ArrayList<>();
        List<Map<String, Object>> myPaidBills = new ArrayList<>();

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
                        if (!split.isPaid() && split.getAmountOwed() != null
                                && split.getAmountOwed().compareTo(BigDecimal.ZERO) > 0) {

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
                            targetCreditor.put("totalAmount", currentTotal.add(split.getAmountOwed()));

                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> items = (List<Map<String, Object>>) targetCreditor.get("items");
                            Map<String, Object> item = new HashMap<>();
                            item.put("title", expense.getTitle() != null ? expense.getTitle() : "ไม่มีชื่อรายการ");
                            item.put("amount", split.getAmountOwed());
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