package com.cp.party_trip.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

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
    @JsonIgnore
    private Trip trip;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private TripMember user;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<ExpenseSplit> expenseSplits; // List of expense splits

    private String title; // Title of the expense
    private BigDecimal totalAmount; // Amount of the expense
    private String currency; // Currency of the expense
    private String splitType; // Type of split (e.g., equal, percentage, custom)

    @Column(name = "expense_date")
    private LocalDateTime expenseDate = LocalDateTime.now(); // Date of the expense (เลือกวันได้ตอนบันทึก/แก้)

    // คนที่บันทึกบิล (อาจบันทึกแทนเพื่อนที่จ่าย) — แก้/ลบได้ทั้งคนจ่ายและคนบันทึก
    @Column(name = "recorded_by_member_id")
    private Long recordedById;

    @Column(name = "category")
    private String category; // FOOD, TRANSPORT, ACCOMMODATION, SHOPPING

    // บิลเงินต่างประเทศ: ยอดตามใบเสร็จ + เรท (totalAmount = ยอดเงินบาทที่ใช้หาร/คิดหนี้)
    @Column(name = "original_amount", precision = 12, scale = 2)
    private BigDecimal originalAmount;

    @Column(name = "exchange_rate", precision = 14, scale = 6)
    private BigDecimal exchangeRate;

    // บิลนี้บันทึกจากรายการไหนในแพลนเที่ยว (null = บิลทั่วไป)
    @Column(name = "activity_id")
    private Long activityId;

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

    public Long getRecordedById() {
        return recordedById;
    }

    public void setRecordedById(Long recordedById) {
        this.recordedById = recordedById;
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

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(BigDecimal originalAmount) {
        this.originalAmount = originalAmount;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

}
