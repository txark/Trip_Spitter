package com.cp.party_trip.dto;

import java.math.BigDecimal;

public class CategorySummaryDTO {
    private String category;
    private BigDecimal totalAmount;

    public CategorySummaryDTO(String category, BigDecimal totalAmount) {
        this.category = category;
        this.totalAmount = totalAmount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}