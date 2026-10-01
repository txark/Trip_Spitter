package com.cp.party_trip.service;

import com.cp.party_trip.model.Trip;
import com.cp.party_trip.model.TripMember;
import com.cp.party_trip.model.User;
import com.cp.party_trip.model.UserTripHistory;
import com.cp.party_trip.repository.TripRepo;
import com.cp.party_trip.repository.TripMemberRepo;
import com.cp.party_trip.repository.UserRepo;
import com.cp.party_trip.repository.UserTripHistoryRepo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
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
        if (creatorName == null || creatorName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "กรุณาระบุชื่อผู้สร้างทริป");
        }
        if (trip.getTitle() == null || trip.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "กรุณาตั้งชื่อทริป");
        }
        // กันผู้ใช้ส่ง id มาเพื่อเขียนทับทริปเดิม หรือแนบสมาชิก/กิจกรรมมาเอง
        trip.setId(null);
        trip.setTripMembers(null);
        trip.setActivities(null);

        User creator = userRepo.findByUsername(creatorName)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setUsername(creatorName);
                    return userRepo.save(newUser);
                });

        trip.setInviteCode(generateInviteCode());
        Trip savedTrip = tripRepo.save(trip);

        TripMember tripMember = new TripMember();
        tripMember.setTrip(savedTrip); // ใช้ setTrip ส่ง Object Trip เข้าไปตรงๆ
        tripMember.setGuestName(creator != null ? creator.getUsername() : "Guest");
        tripMember.setRole("ADMIN");
        tripMemberRepo.save(tripMember);

        // บันทึกประวัติการเข้าร่วมทริปของผู้ใช้
        UserTripHistory history = new UserTripHistory();
        history.setUserId(creator != null ? creator.getId() : null);
        history.setTripId(savedTrip.getId());
        history.setViewedAt(java.time.LocalDateTime.now());
        userTripHistoryRepo.save(history);
        System.out.println("===== บันทึกประวัติสำเร็จสำหรับ User ID: " + (creator != null ? creator.getId() : null)
                + " และ Trip ID: "
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

        Trip trip = tripRepo.findByInviteCodeIgnoreCase(inviteCode.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ไม่พบทริปที่ตรงกับรหัสเชิญนี้"));

        // เข้าร่วมซ้ำ (หรือคนสร้างกรอกรหัสของตัวเอง) = ใช้สมาชิกเดิม ไม่สร้างคนซ้ำ
        String guestName = user != null ? user.getUsername() : "Guest";
        TripMember savedMember = tripMemberRepo.findFirstByTripIdAndGuestNameOrderByIdAsc(trip.getId(), guestName)
                .orElseGet(() -> {
                    TripMember tripMember = new TripMember();
                    tripMember.setTrip(trip); // ใช้ setTrip ส่ง Object Trip เข้าไปตรงๆ
                    tripMember.setGuestName(guestName);
                    tripMember.setRole("MEMBER");
                    return tripMemberRepo.save(tripMember);
                });

        // บันทึกประวัติการเข้าร่วมทริปของผู้ใช้ (ข้อมูลเก่าอาจซ้ำหลายแถว ใช้แถวล่าสุด)
        List<UserTripHistory> rows = userTripHistoryRepo.findByUserIdAndTripIdOrderByViewedAtDesc(user.getId(),
                trip.getId());
        UserTripHistory history = rows.isEmpty() ? new UserTripHistory() : rows.get(0);
        history.setUserId(user.getId());
        history.setTripId(trip.getId());
        history.setViewedAt(java.time.LocalDateTime.now());
        userTripHistoryRepo.save(history);
        System.out.println(
                "===== บันทึกประวัติสำเร็จสำหรับ User ID: " + (user != null ? user.getId() : null) + " และ Trip ID: "
                        + trip.getId() + " =====");

        return savedMember;
    }

    public Trip getTripById(Long id) {
        return tripRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ไม่พบข้อมูลทริปที่มีรหัส: " + id));
    }

    // รหัสเชิญ 6 ตัว สุ่มใหม่จนกว่าจะไม่ซ้ำกับทริปอื่น (ซ้ำแล้วค้นหาด้วยรหัสจะ error ทั้งสองทริป)
    private String generateInviteCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String code = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
            if (!tripRepo.existsByInviteCodeIgnoreCase(code)) {
                return code;
            }
        }
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "สร้างรหัสเชิญไม่สำเร็จ กรุณาลองใหม่");
    }
}