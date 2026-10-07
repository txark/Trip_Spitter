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
        return ActivityResponse.builder()
                .id(a.getId()).title(a.getTitle()).category(a.getCategory())
                .activityDate(a.getActivityDate()).endDate(a.getEndDate())
                .startTime(a.getStartTime()).endTime(a.getEndTime())
                .location(a.getLocation()).latitude(a.getLatitude()).longitude(a.getLongitude())
                .notes(a.getNotes()).origin(a.getOrigin()).transportMode(a.getTransportMode()).stops(stops)
                .mealType(a.getMealType()).rooms(a.getRooms()).guestsPerRoom(a.getGuestsPerRoom())
                .bookingMethod(a.getBookingMethod()).bookingRef(a.getBookingRef()).booked(a.getBooked())
                .contact(a.getContact()).cost(a.getCost()).costCurrency(a.getCostCurrency())
                .costRate(a.getCostRate()).participantIds(List.copyOf(a.getParticipantIds()))
                .pollId(a.getPollId()).createdByMemberId(a.getCreatedByMemberId()).createdAt(a.getCreatedAt())
                .build();
    }

    public List<ActivityResponse> toResponses(List<Activity> activities) {
        return activities.stream().map(this::toResponse).toList();
    }
}
