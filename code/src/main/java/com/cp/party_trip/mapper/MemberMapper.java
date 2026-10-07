package com.cp.party_trip.mapper;

import com.cp.party_trip.dto.response.MemberRefResponse;
import com.cp.party_trip.dto.response.MemberResponse;
import com.cp.party_trip.model.TripMember;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MemberMapper {
    public MemberResponse toResponse(TripMember member) {
        if (member == null) {
            return null;
        }
        return new MemberResponse(member.getId(), member.getGuestName(), member.getUserName(), member.getRole(),
                member.getJoinedAt());
    }

    public List<MemberResponse> toResponses(List<TripMember> members) {
        return members == null ? List.of() : members.stream().map(this::toResponse).toList();
    }

    public MemberRefResponse toRef(TripMember member) {
        return member == null ? null : new MemberRefResponse(member.getId(), member.getGuestName());
    }
}
