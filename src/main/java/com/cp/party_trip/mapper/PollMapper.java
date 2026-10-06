package com.cp.party_trip.mapper;

import com.cp.party_trip.dto.response.PollOptionResponse;
import com.cp.party_trip.dto.response.PollResponse;
import com.cp.party_trip.model.Poll;
import com.cp.party_trip.model.PollOption;
import org.springframework.stereotype.Component;

@Component
public class PollMapper {
    public PollResponse toResponse(Poll p) {
        return new PollResponse(p.getId(), p.getTripId(), p.getQuestion(), p.getStatus(), p.getCreatedByMemberId(),
                p.getCreatedAt(), p.getClosesAt());
    }

    public PollOptionResponse toResponse(PollOption o) {
        return new PollOptionResponse(o.getId(), o.getPollId(), o.getOptionText());
    }
}
