package com.cp.party_trip.service;

import com.cp.party_trip.model.Trip;
import com.cp.party_trip.model.TripMember;
import com.cp.party_trip.model.User;
import com.cp.party_trip.model.UserTripHistory;
import com.cp.party_trip.repository.TripRepo;
import com.cp.party_trip.repository.TripMemberRepo;
import com.cp.party_trip.repository.UserRepo;
import com.cp.party_trip.repository.UserTripHistoryRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class TripService {

    private final TripRepo tripRepo;
    private final TripMemberRepo tripMemberRepo;
    private final UserRepo userRepo;
    private final UserTripHistoryRepo userTripHistoryRepo;

    public TripService(TripRepo tripRepo, TripMemberRepo tripMemberRepo, UserRepo userRepo,
            UserTripHistoryRepo userTripHistoryRepo) {
        this.tripRepo = tripRepo;
        this.tripMemberRepo = tripMemberRepo;
        this.userRepo = userRepo;
        this.userTripHistoryRepo = userTripHistoryRepo;
    }

    @Transactional
    public Trip createTrip(Trip trip, String creatorName) {
        User creator = userRepo.findByUsername(creatorName)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setUsername(creatorName);
                    return userRepo.save(newUser);
                });

        String inviteCode = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        trip.setInviteCode(inviteCode);
        Trip savedTrip = tripRepo.save(trip);

        TripMember tripMember = new TripMember();
        tripMember.setTrip(savedTrip); // ใช้ setTrip ส่ง Object Trip เข้าไปตรงๆ
        tripMember.setGuestName(creator.getUsername());
        tripMember.setRole("ADMIN");
        tripMemberRepo.save(tripMember);

        // บันทึกประวัติการเข้าร่วมทริปของผู้ใช้
        UserTripHistory history = new UserTripHistory();
        history.setUserId(creator.getId());
        history.setTripId(savedTrip.getId());
        history.setViewedAt(java.time.LocalDateTime.now());
        userTripHistoryRepo.save(history);
        System.out.println("===== บันทึกประวัติสำเร็จสำหรับ User ID: " + creator.getId() + " และ Trip ID: "
                + savedTrip.getId() + " =====");

        return savedTrip;
    }

    @Transactional
    public TripMember joinTrip(String inviteCode, String userName) {
        // บันทึกหรือค้นหา User
        User user = userRepo.findByUsername(userName)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setUsername(userName);
                    return userRepo.save(newUser);
                });

        Trip trip = tripRepo.findByInviteCode(inviteCode)
                .orElseThrow(() -> new RuntimeException("ไม่พบทริปที่ตรงกับรหัสเชิญนี้"));

        TripMember tripMember = new TripMember();
        tripMember.setTrip(trip); // ใช้ setTrip ส่ง Object Trip เข้าไปตรงๆ
        tripMember.setGuestName(user.getUsername());
        tripMember.setRole("MEMBER");
        TripMember savedMember = tripMemberRepo.save(tripMember);

        // บันทึกประวัติการเข้าร่วมทริปของผู้ใช้
        UserTripHistory history = new UserTripHistory();
        history.setUserId(user.getId());
        history.setTripId(trip.getId());
        history.setViewedAt(java.time.LocalDateTime.now());
        userTripHistoryRepo.save(history);
        System.out.println("===== บันทึกประวัติสำเร็จสำหรับ User ID: " + user.getId() + " และ Trip ID: "
                + trip.getId() + " =====");

        return savedMember;
    }

    public Trip getTripById(Long id) {
        return tripRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("ไม่พบข้อมูลทริปที่มีรหัส: " + id));
    }
}