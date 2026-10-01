package com.cp.party_trip.service;

import com.cp.party_trip.dto.DebtTransfer;
import com.cp.party_trip.model.Expense;
import com.cp.party_trip.model.ExpenseSplit;
import com.cp.party_trip.model.Settlement;
import com.cp.party_trip.model.TripMember;
import com.cp.party_trip.repository.ExpenseRepo;
import com.cp.party_trip.repository.SettlementRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class DebtService {
    private final ExpenseRepo expenseRepo;
    private final SettlementRepo settlementRepo;

    public DebtService(ExpenseRepo expenseRepo, SettlementRepo settlementRepo) {
        this.expenseRepo = expenseRepo;
        this.settlementRepo = settlementRepo;
    }

    // ใช้ TripMember เป็น key ของ Map ได้เพราะอยู่ใน transaction เดียวกัน (ได้ object ตัวเดิมเสมอ)
    // และโหลด expenseSplits แบบ lazy ได้โดยไม่ต้องพึ่ง open-in-view
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
                // ส่วนที่ผู้จ่ายยืนยันแล้วว่าได้รับเงิน ถือว่าชำระแล้ว ไม่นับเป็นหนี้
                if (split.isPaid() && !owedby.equals(paidBy)) {
                    balances.put(paidBy, balances.get(paidBy).subtract(split.getAmountOwed()));
                    continue;
                }
                balances.put(owedby, balances.getOrDefault(owedby, BigDecimal.ZERO).subtract(split.getAmountOwed()));
            }
        }

        List<Settlement> settlements = settlementRepo.findByTripId(tripId);
        for (Settlement settlement : settlements) {
            TripMember sender = settlement.getSender();
            TripMember receiver = settlement.getReceiver();
            BigDecimal amount = settlement.getAmount();

            balances.put(sender, balances.getOrDefault(sender, BigDecimal.ZERO).add(amount));
            balances.put(receiver, balances.getOrDefault(receiver, BigDecimal.ZERO).subtract(amount));
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
}
