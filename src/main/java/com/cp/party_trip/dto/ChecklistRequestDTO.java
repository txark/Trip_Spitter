package com.cp.party_trip.dto;

public class ChecklistRequestDTO {
    private Long tripId;
    private String category;
    private String itemName;
    private Long assignedToMemberId;
    private String notes;

    // Getters & Setters
    public Long getTripId() {
        return tripId;
    }

    public String getCategory() {
        return category;
    }

    public String getItemName() {
        return itemName;
    }

    public Long getAssignedToMemberId() {
        return assignedToMemberId;
    }

    public String getNotes() {
        return notes;
    }
}