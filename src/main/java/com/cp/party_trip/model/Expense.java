package com.cp.party_trip.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "expenses")
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private TripMember user;

    private String title; // Title of the expense
    private BigDecimal totalAmount; // Amount of the expense
    private String currency; // Currency of the expense
    private String splitType; // Type of split (e.g., equal, percentage, custom)

    @Column(name = "expense_date", updatable = false)
    private LocalDateTime expenseDate = LocalDateTime.now(); // Date of the expense

    @Column(name = "category")
    private String category; // FOOD, TRANSPORT, ACCOMMODATION, SHOPPING

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExpenseSplit> expenseSplits; // List of expense splits

    // Getters & Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Trip getTrip() {
        return trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }

    public TripMember getUser() {
        return user;
    }

    public void setUser(TripMember user) {
        this.user = user;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getSplitType() {
        return splitType;
    }

    public void setSplitType(String splitType) {
        this.splitType = splitType;
    }

    public LocalDateTime getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDateTime expenseDate) {
        this.expenseDate = expenseDate;
    }

    public List<ExpenseSplit> getExpenseSplits() {
        return expenseSplits;
    }

    public void setExpenseSplits(List<ExpenseSplit> expenseSplits) {
        this.expenseSplits = expenseSplits;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

}
