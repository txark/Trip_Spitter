package com.cp.party_trip.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "expense_splits")
public class ExpenseSplit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "expense_id")
    private Expense expense;

    @ManyToOne
    @JoinColumn(name = "trip_member_id")
    private TripMember tripMember;

    private BigDecimal amountOwed; // Amount for this split
    private BigDecimal percentage; // Percentage of the total expense this member is responsible for

    // Getters & Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Expense getExpense() {
        return expense;
    }

    public void setExpense(Expense expense) {
        this.expense = expense;
    }

    public TripMember getTripMember() {
        return tripMember;
    }

    public void setTripMember(TripMember tripMember) {
        this.tripMember = tripMember;
    }

    public BigDecimal getAmountOwed() {
        return amountOwed;
    }

    public void setAmountOwed(BigDecimal amountOwed) {
        this.amountOwed = amountOwed;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }
}
