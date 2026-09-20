package com.cp.party_trip.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SettlementDTO {
    private Long id;
    private String senderName; // ชื่อผู้โอน
    private String receiverName; // ชื่อผู้รับโอน
    private BigDecimal amount;
    private LocalDateTime settledAt;

    public SettlementDTO(Long id, String senderName, String receiverName, BigDecimal amount, LocalDateTime settledAt) {
        this.id = id;
        this.senderName = senderName;
        this.receiverName = receiverName;
        this.amount = amount;
        this.settledAt = settledAt;
    }

    // Getters & Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getSettledAt() {
        return settledAt;
    }

    public void setSettledAt(LocalDateTime settledAt) {
        this.settledAt = settledAt;
    }
}