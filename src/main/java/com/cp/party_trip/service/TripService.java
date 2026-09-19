package com.cp.party_trip.service;

import com.cp.party_trip.model.Trip;
import com.cp.party_trip.model.TripMember;
import com.cp.party_trip.repository.TripRepo;
import com.cp.party_trip.repository.TripMemberRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class TripService {

    private final TripRepo tripRepo;
    private final TripMemberRepo tripMemberRepo;

    public TripService(TripRepo tripRepo, TripMemberRepo tripMemberRepo) {
        this.tripRepo = tripRepo;
        this.tripMemberRepo = tripMemberRepo;
    }

    @Transactional
    public Trip createTrip(Trip trip, String creatorName) {
        // random invite code (6 characters 0-9, a-z, A-Z)
        String inviteCode = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        trip.setInviteCode(inviteCode);
        Trip savedTrip = tripRepo.save(trip);

        // role of the creator is "ADMIN"
        TripMember tripMember = new TripMember();
        tripMember.setTrip(savedTrip);
        tripMember.setUserName(creatorName);
        tripMember.setRole("ADMIN");
        tripMemberRepo.save(tripMember);

        return savedTrip;
    }

    @Transactional
    public TripMember joinTrip(String inviteCode, String userName) {
        Trip trip = tripRepo.findByInviteCode(inviteCode)
                .orElseThrow(() -> new RuntimeException("Trip not found with the provided invite code"));

        // role of the new member is "MEMBER"
        TripMember tripMember = new TripMember();
        tripMember.setTrip(trip);
        tripMember.setUserName(userName);
        tripMember.setRole("MEMBER");

        return tripMemberRepo.save(tripMember);
    }
}
