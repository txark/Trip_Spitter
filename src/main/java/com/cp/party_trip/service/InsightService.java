package com.cp.party_trip.service;

import com.cp.party_trip.dto.CategorySummaryDTO;
import com.cp.party_trip.dto.DailySummaryDTO;
import com.cp.party_trip.repository.ExpenseRepo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class InsightService {

    private final ExpenseRepo expenseRepo;

    public InsightService(ExpenseRepo expenseRepo) {
        this.expenseRepo = expenseRepo;
    }

    public List<CategorySummaryDTO> getCategoryInsights(Long tripId) {
        List<Object[]> results = expenseRepo.sumAmountByCategory(tripId);
        List<CategorySummaryDTO> summaries = new ArrayList<>();

        for (Object[] row : results) {
            String category = row[0] != null ? row[0].toString() : "OTHER";
            // เพิ่มการตรวจสอบเงื่อนไข row[1] != null เพื่อป้องกันระบบแครช
            BigDecimal total = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
            summaries.add(new CategorySummaryDTO(category, total));
        }
        return summaries;
    }

    public List<DailySummaryDTO> getDailyInsights(Long tripId) {
        List<Object[]> results = expenseRepo.sumAmountByDate(tripId);
        List<DailySummaryDTO> summaries = new ArrayList<>();

        for (Object[] row : results) {
            java.sql.Date sqlDate = (java.sql.Date) row[0];
            // เพิ่มการตรวจสอบเงื่อนไข row[1] != null เพื่อป้องกันระบบแครช
            BigDecimal total = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
            summaries.add(new DailySummaryDTO(sqlDate.toLocalDate(), total));
        }
        return summaries;
    }
}