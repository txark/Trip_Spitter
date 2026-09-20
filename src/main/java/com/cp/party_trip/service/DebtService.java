package com.cp.party_trip.service;

import com.cp.party_trip.dto.DebtTransfer;
import com.cp.party_trip.model.Expense;
import com.cp.party_trip.model.ExpenseSplit;
import com.cp.party_trip.model.TripMember;
import com.cp.party_trip.repository.ExpenseRepo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class DebtService {
    private final ExpenseRepo expenseRepo;

    public DebtService(ExpenseRepo expenseRepo) {
        this.expenseRepo = expenseRepo;
    }

    public List<DebtTransfer> calculateDebtSimplification(Long tripId) {
        List<Expense> expenses = expenseRepo.findByTripId(tripId);
        Map<TripMember, BigDecimal> balances = new HashMap<>();

        // คำนวณยอดเงินของสมาชิกทุกคน
        for (Expense expense : expenses) {
            TripMember paidBy = expense.getUser();
            balances.put(paidBy, balances.getOrDefault(paidBy, BigDecimal.ZERO).add(expense.getTotalAmount()));

            for (ExpenseSplit split : expense.getExpenseSplits()) {
                TripMember owedby = split.getTripMember();
                balances.put(owedby, balances.getOrDefault(owedby, BigDecimal.ZERO).subtract(split.getAmountOwed()));
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

        // จับคู่ล้างหนี้
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
