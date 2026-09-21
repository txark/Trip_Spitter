package com.cp.party_trip.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DailySummaryDTO {
    private LocalDate expenseDate;
    private BigDecimal dailyTotal;

    public DailySummaryDTO(LocalDate expenseDate, BigDecimal dailyTotal) {
        this.expenseDate = expenseDate;
        this.dailyTotal = dailyTotal;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }

    public BigDecimal getDailyTotal() {
        return dailyTotal;
    }

    public void setDailyTotal(BigDecimal dailyTotal) {
        this.dailyTotal = dailyTotal;
    }
}