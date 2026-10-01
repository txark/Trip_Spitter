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
            // Hibernate 7 คืน LocalDate ส่วนเวอร์ชันเก่าคืน java.sql.Date รองรับทั้งสองแบบ
            java.time.LocalDate date = row[0] instanceof java.sql.Date sqlDate
                    ? sqlDate.toLocalDate()
                    : (java.time.LocalDate) row[0];
            // เพิ่มการตรวจสอบเงื่อนไข row[1] != null เพื่อป้องกันระบบแครช
            BigDecimal total = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
            summaries.add(new DailySummaryDTO(date, total));
        }
        return summaries;
    }
}