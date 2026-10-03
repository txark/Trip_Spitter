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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.ZoneId;
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
        // สกุลเงิน/เขตเวลาที่เลือกตอนสร้าง: ตรวจแบบเดียวกับตอนแก้ทีหลัง
        String code = Money.currency(trip.getCurrency());
        trip.setCurrency(code);
        trip.setExchangeRate(code == null ? null : Money.rate(trip.getExchangeRate()));
        trip.setTimeZone(validZoneOrNull(trip.getTimeZone()));

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

    static final BigDecimal MAX_BUDGET = new BigDecimal("9999999");

    // ตั้งงบต่อคน: ไม่ส่ง / 0 = ยกเลิกงบ
    @Transactional
    public Trip updateBudget(Long tripId, Long memberId, BigDecimal amount) {
        Trip trip = getTripById(tripId);
        requireMember(tripId, memberId, "เฉพาะสมาชิกในทริปเท่านั้นที่ตั้งงบได้");
        if (amount == null || amount.signum() == 0) {
            trip.setBudgetPerPerson(null);
        } else if (amount.signum() < 0 || amount.compareTo(MAX_BUDGET) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "งบต้องอยู่ระหว่าง 1 ถึง 9,999,999 บาท");
        } else if (amount.stripTrailingZeros().scale() > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "งบใส่ทศนิยมได้ไม่เกิน 2 ตำแหน่ง");
        } else {
            trip.setBudgetPerPerson(amount.setScale(2, RoundingMode.UNNECESSARY));
        }
        return tripRepo.save(trip);
    }

    // ตั้งสกุลเงินท้องถิ่นของทริป: ไม่ส่ง / THB = ใช้บาทอย่างเดียว
    @Transactional
    public Trip updateCurrency(Long tripId, Long memberId, String currency, BigDecimal rate) {
        Trip trip = getTripById(tripId);
        requireMember(tripId, memberId, "เฉพาะสมาชิกในทริปเท่านั้นที่ตั้งสกุลเงินได้");
        String code = Money.currency(currency);
        if (code == null) {
            trip.setCurrency(null);
            trip.setExchangeRate(null);
        } else {
            trip.setCurrency(code);
            trip.setExchangeRate(Money.rate(rate));
        }
        return tripRepo.save(trip);
    }

    private void requireMember(Long tripId, Long memberId, String message) {
        boolean member = memberId != null && tripMemberRepo.findById(memberId)
                .map(m -> m.getTrip() != null && tripId.equals(m.getTrip().getId()))
                .orElse(false);
        if (!member) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        }
    }

    // เปลี่ยนเขตเวลาของทริป: สมาชิกคนไหนก็เปลี่ยนได้ (เหมือนแก้แพลน)
    @Transactional
    public Trip updateTimeZone(Long tripId, Long memberId, String timeZone) {
        Trip trip = getTripById(tripId);
        requireMember(tripId, memberId, "เฉพาะสมาชิกในทริปเท่านั้นที่เปลี่ยนเขตเวลาได้");
        String zone = timeZone == null ? "" : timeZone.trim();
        // รับเฉพาะชื่อเขตเวลาแบบ ทวีป/เมือง (ไม่รับ +07:00 หรือ UTC เฉย ๆ ที่ไม่ปรับเวลาออมแสง)
        try {
            if (!zone.contains("/") || zone.length() > 50) {
                throw new DateTimeException(zone);
            }
            trip.setTimeZone(ZoneId.of(zone).getId());
        } catch (DateTimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "เขตเวลาไม่ถูกต้อง");
        }
        return tripRepo.save(trip);
    }

    // เขตเวลาที่ส่งมาตอนสร้างทริป: ไม่ถูกต้อง = ใช้ค่าเริ่มต้น (กรุงเทพ) แทนที่จะสร้างไม่ได้
    private String validZoneOrNull(String zone) {
        if (zone == null || !zone.contains("/") || zone.length() > 50) {
            return null;
        }
        try {
            return ZoneId.of(zone.trim()).getId();
        } catch (DateTimeException e) {
            return null;
        }
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