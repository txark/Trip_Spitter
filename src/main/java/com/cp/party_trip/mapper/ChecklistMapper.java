package com.cp.party_trip.mapper;

import com.cp.party_trip.dto.response.ChecklistItemResponse;
import com.cp.party_trip.model.ChecklistItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChecklistMapper {
    public ChecklistItemResponse toResponse(ChecklistItem i) {
        return new ChecklistItemResponse(i.getId(), i.getTripId(), i.getCategory(), i.getItemName(), i.getQuantity(),
                i.getUnit(), i.getNotes(), i.getAssignedToMemberId(), List.copyOf(i.getAssigneeIds()), i.isChecked(),
                i.getUpdatedByMemberId());
    }

    public List<ChecklistItemResponse> toResponses(List<ChecklistItem> items) {
        return items.stream().map(this::toResponse).toList();
    }
}
