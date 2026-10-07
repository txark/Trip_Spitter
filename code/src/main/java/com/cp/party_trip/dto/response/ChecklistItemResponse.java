package com.cp.party_trip.dto.response;

import java.math.BigDecimal;
import java.util.List;

// ของในเช็กลิสต์
public record ChecklistItemResponse(
        Long id,
        Long tripId,
        String category,
        String itemName,
        BigDecimal quantity,
        String unit,
        String notes,
        Long assignedToMemberId,
        List<Long> assigneeIds,
        boolean checked,
        Long updatedByMemberId) {
}
