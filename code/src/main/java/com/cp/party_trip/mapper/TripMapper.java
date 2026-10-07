package com.cp.party_trip.mapper;

import com.cp.party_trip.dto.request.CreateTripRequest;
import com.cp.party_trip.dto.response.TripHistoryResponse;
import com.cp.party_trip.dto.response.TripResponse;
import com.cp.party_trip.model.Trip;
import com.cp.party_trip.model.UserTripHistory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TripMapper {
    private final MemberMapper memberMapper;

    public TripMapper(MemberMapper memberMapper) {
        this.memberMapper = memberMapper;
    }

    // request -> entity ใหม่ (id/สมาชิก/กิจกรรมมาจาก request ไม่ได้)
    public Trip toEntity(CreateTripRequest request) {
        Trip trip = new Trip();
        trip.setTitle(request.getTitle());
        trip.setStartDate(request.getStartDate());
        trip.setEndDate(request.getEndDate());
        trip.setCurrency(request.getCurrency());
        trip.setExchangeRate(request.getExchangeRate());
        trip.setTimeZone(request.getTimeZone());
        return trip;
    }

    public TripResponse toResponse(Trip trip) {
        return new TripResponse(trip.getId(), trip.getTitle(), trip.getStartDate(), trip.getEndDate(),
                trip.getInviteCode(), trip.getTimeZone(), trip.getBudgetPerPerson(), trip.getCurrency(),
                trip.getExchangeRate(), trip.getCreatedAt(), memberMapper.toResponses(trip.getTripMembers()));
    }

    public TripHistoryResponse toResponse(UserTripHistory history) {
        return new TripHistoryResponse(history.getId(), history.getUserId(), history.getTripId(),
                history.getViewedAt());
    }

    public List<TripHistoryResponse> toHistoryResponses(List<UserTripHistory> histories) {
        return histories.stream().map(this::toResponse).toList();
    }
}
