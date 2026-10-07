package com.cp.party_trip.mapper;

import com.cp.party_trip.dto.response.TripEventResponse;
import com.cp.party_trip.model.TripEvent;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TripEventMapper {
    public TripEventResponse toResponse(TripEvent e) {
        return new TripEventResponse(e.getId(), e.getMemberId(), e.getEventType(), e.getMessage(), e.getCreatedAt());
    }

    public List<TripEventResponse> toResponses(List<TripEvent> events) {
        return events.stream().map(this::toResponse).toList();
    }
}
