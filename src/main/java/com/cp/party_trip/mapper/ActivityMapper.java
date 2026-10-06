package com.cp.party_trip.mapper;

import com.cp.party_trip.dto.response.ActivityResponse;
import com.cp.party_trip.model.Activity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ActivityMapper {
    public ActivityResponse toResponse(Activity a) {
        List<ActivityResponse.Stop> stops = a.getStops() == null ? List.of()
                : a.getStops().stream()
                        .map(s -> new ActivityResponse.Stop(s.getPlace(), s.getArriveTime(), s.getDepartTime()))
                        .toList();
        return new ActivityResponse(a.getId(), a.getTitle(), a.getCategory(), a.getActivityDate(), a.getEndDate(),
                a.getStartTime(), a.getEndTime(), a.getLocation(), a.getLatitude(), a.getLongitude(), a.getNotes(),
                a.getOrigin(), a.getTransportMode(), stops, a.getMealType(), a.getRooms(), a.getGuestsPerRoom(),
                a.getBookingMethod(), a.getBookingRef(), a.getBooked(), a.getContact(), a.getCost(),
                a.getCostCurrency(), a.getCostRate(), List.copyOf(a.getParticipantIds()), a.getPollId(),
                a.getCreatedByMemberId(), a.getCreatedAt());
    }

    public List<ActivityResponse> toResponses(List<Activity> activities) {
        return activities.stream().map(this::toResponse).toList();
    }
}
