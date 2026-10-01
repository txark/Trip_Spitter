package com.cp.party_trip.service;

import com.cp.party_trip.dto.ActivityRequest;
import com.cp.party_trip.model.Activity;
import com.cp.party_trip.model.Trip;
import com.cp.party_trip.model.TripMember;
import com.cp.party_trip.repository.ActivityRepo;
import com.cp.party_trip.repository.TripMemberRepo;
import com.cp.party_trip.repository.TripRepo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ActivityService {
    static final int MAX_TITLE_LENGTH = 100;
    static final int MAX_LOCATION_LENGTH = 150;
    static final int MAX_NOTES_LENGTH = 500;
    static final String DEFAULT_CATEGORY = "OTHER";
    static final Set<String> CATEGORIES = Set.of("TRAVEL", "FOOD", "STAY", "SIGHTSEEING", "ACTIVITY", "OTHER");
    static final Set<String> TRANSPORT_MODES = Set.of("CAR", "VAN", "BUS", "TRAIN", "PLANE", "BOAT", "OTHER");
    static final Set<String> MEAL_TYPES = Set.of("BREAKFAST", "LUNCH", "DINNER", "SNACK", "LATE");
    // วิธีเดินทางที่มีเลขเที่ยว/ที่นั่ง/คิวรถ (รถส่วนตัวไม่มี)
    static final Set<String> BOOKABLE_MODES = Set.of("VAN", "BUS", "TRAIN", "PLANE", "BOAT");
    static final int MAX_BOOKING_REF_LENGTH = 100;
    static final int MAX_CONTACT_LENGTH = 100;
    static final int MAX_ROOMS = 50;
    static final int MAX_GUESTS_PER_ROOM = 20;
    static final Set<String> BOOKING_METHODS = Set.of("ONLINE", "PHONE", "PAGE");
    static final BigDecimal MAX_COST = new BigDecimal("9999999");

    // เรียงตามวัน → กิจกรรมไม่ระบุเวลาก่อน → เวลาเริ่ม → ลำดับที่เพิ่ม
    private static final Comparator<Activity> TIMELINE_ORDER = Comparator
            .comparing(Activity::getActivityDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Activity::getStartTime, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(Activity::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    private final ActivityRepo activityRepo;
    private final TripRepo tripRepo;
    private final TripMemberRepo tripMemberRepo;

    public ActivityService(ActivityRepo activityRepo, TripRepo tripRepo, TripMemberRepo tripMemberRepo) {
        this.activityRepo = activityRepo;
        this.tripRepo = tripRepo;
        this.tripMemberRepo = tripMemberRepo;
    }

    @Transactional(readOnly = true)
    public List<Activity> getTripActivities(Long tripId) {
        requireTrip(tripId);
        return activityRepo.findByTripId(tripId).stream().sorted(TIMELINE_ORDER).toList();
    }

    @Transactional
    public Activity addActivity(Long tripId, ActivityRequest request) {
        Trip trip = requireTrip(tripId);
        requireTripMember(tripId, request == null ? null : request.getMemberId());
        Activity activity = new Activity();
        activity.setTrip(trip);
        activity.setCreatedByMemberId(request.getMemberId());
        apply(activity, trip, request);
        return activityRepo.save(activity);
    }

    @Transactional
    public Activity updateActivity(Long activityId, ActivityRequest request) {
        Activity activity = requireActivity(activityId);
        Trip trip = activity.getTrip();
        requireTripMember(trip.getId(), request == null ? null : request.getMemberId());
        apply(activity, trip, request);
        return activityRepo.save(activity);
    }

    @Transactional
    public void deleteActivity(Long activityId, Long memberId) {
        Activity activity = requireActivity(activityId);
        requireTripMember(activity.getTrip().getId(), memberId);
        activityRepo.delete(activity);
    }

    private void apply(Activity activity, Trip trip, ActivityRequest request) {
        String category = clean(request.getCategory());
        category = category != null && CATEGORIES.contains(category) ? category : DEFAULT_CATEGORY;
        boolean stay = "STAY".equals(category);

        String title = clean(request.getTitle());
        if (title == null) {
            throw badRequest("กรุณากรอกชื่อกิจกรรม");
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            throw badRequest("ชื่อกิจกรรมยาวเกิน " + MAX_TITLE_LENGTH + " ตัวอักษร");
        }

        LocalDate date = request.getActivityDate();
        if (date == null) {
            throw badRequest("กรุณาเลือกวันของกิจกรรม");
        }
        if ((trip.getStartDate() != null && date.isBefore(trip.getStartDate()))
                || (trip.getEndDate() != null && date.isAfter(trip.getEndDate()))) {
            throw badRequest("วันของกิจกรรมต้องอยู่ในช่วงวันเที่ยวของทริป");
        }

        LocalTime start = minutes(request.getStartTime());
        LocalTime end = minutes(request.getEndTime());
        LocalDate endDate = null;
        if (stay) {
            // ที่พัก: เช็คอิน (วัน+เวลา) → เช็คเอาท์ (วัน+เวลา) ข้ามได้หลายวัน
            endDate = request.getEndDate() != null ? request.getEndDate() : date;
            if (endDate.isBefore(date)) {
                throw badRequest("วันเช็คเอาท์ต้องไม่ก่อนวันเช็คอิน");
            }
            if (trip.getEndDate() != null && endDate.isAfter(trip.getEndDate())) {
                throw badRequest("วันเช็คเอาท์ต้องอยู่ในช่วงวันเที่ยวของทริป");
            }
            if (endDate.equals(date) && start != null && end != null && !end.isAfter(start)) {
                throw badRequest("เวลาเช็คเอาท์ต้องหลังเวลาเช็คอิน");
            }
        } else if ("FOOD".equals(category)) {
            end = null; // มื้ออาหารมีแค่เวลาเริ่ม
        } else {
            // ประเภทอื่น: เวลาจบน้อยกว่าเวลาเริ่ม = จบวันถัดไป
            if (end != null && start == null) {
                throw badRequest("กรุณาใส่เวลาเริ่มก่อนเวลาสิ้นสุด");
            }
            if (end != null && end.equals(start)) {
                throw badRequest("เวลาสิ้นสุดต้องไม่เท่ากับเวลาเริ่ม");
            }
        }

        String location = clean(request.getLocation());
        if (location != null && location.length() > MAX_LOCATION_LENGTH) {
            throw badRequest("สถานที่ยาวเกิน " + MAX_LOCATION_LENGTH + " ตัวอักษร");
        }
        String notes = clean(request.getNotes());
        if (notes != null && notes.length() > MAX_NOTES_LENGTH) {
            throw badRequest("โน้ตยาวเกิน " + MAX_NOTES_LENGTH + " ตัวอักษร");
        }

        String origin = null;
        String transportMode = null;
        String mealType = null;
        String bookingRef = null;
        Boolean booked = null;
        Integer rooms = null;
        Integer guestsPerRoom = null;
        String bookingMethod = null;
        String contact = null;
        List<Long> participants = List.of();
        switch (category) {
            case "TRAVEL" -> {
                origin = clean(request.getOrigin());
                if (origin != null && origin.length() > MAX_LOCATION_LENGTH) {
                    throw badRequest("ต้นทางยาวเกิน " + MAX_LOCATION_LENGTH + " ตัวอักษร");
                }
                transportMode = oneOf(request.getTransportMode(), TRANSPORT_MODES);
                if (transportMode != null && BOOKABLE_MODES.contains(transportMode)) {
                    bookingRef = cleanBookingRef(request.getBookingRef());
                }
            }
            case "FOOD" -> mealType = oneOf(request.getMealType(), MEAL_TYPES);
            case "STAY" -> {
                rooms = request.getRooms();
                if (rooms != null && (rooms < 1 || rooms > MAX_ROOMS)) {
                    throw badRequest("จำนวนห้องต้องอยู่ระหว่าง 1 ถึง " + MAX_ROOMS);
                }
                guestsPerRoom = request.getGuestsPerRoom();
                if (guestsPerRoom != null && (guestsPerRoom < 1 || guestsPerRoom > MAX_GUESTS_PER_ROOM)) {
                    throw badRequest("จำนวนคนต่อห้องต้องอยู่ระหว่าง 1 ถึง " + MAX_GUESTS_PER_ROOM);
                }
            }
            case "ACTIVITY" -> {
                contact = clean(request.getContact());
                if (contact != null && contact.length() > MAX_CONTACT_LENGTH) {
                    throw badRequest("ข้อมูลติดต่อยาวเกิน " + MAX_CONTACT_LENGTH + " ตัวอักษร");
                }
                participants = cleanParticipants(trip.getId(), request.getParticipantIds());
            }
            default -> {
            }
        }

        // กิน/ที่พัก/กิจกรรม: จองแล้วหรือยัง และจองช่องทางไหน (โทร/เพจ ไม่มีเลขการจองก็ได้)
        if (Set.of("FOOD", "STAY", "ACTIVITY").contains(category)) {
            bookingMethod = oneOf(request.getBookingMethod(), BOOKING_METHODS);
            booked = bookingMethod != null || Boolean.TRUE.equals(request.getBooked());
            bookingRef = booked ? cleanBookingRef(request.getBookingRef()) : null;
        }

        BigDecimal cost = request.getCost();
        if (cost != null) {
            if (cost.signum() < 0 || cost.compareTo(MAX_COST) > 0) {
                throw badRequest("ค่าใช้จ่ายต้องอยู่ระหว่าง 0 ถึง 9,999,999 บาท");
            }
            if (cost.stripTrailingZeros().scale() > 2) {
                throw badRequest("ค่าใช้จ่ายใส่ทศนิยมได้ไม่เกิน 2 ตำแหน่ง");
            }
            cost = cost.setScale(2, RoundingMode.UNNECESSARY);
        }

        activity.setTitle(title);
        activity.setActivityDate(date);
        activity.setStartTime(start);
        activity.setEndTime(end);
        activity.setLocation(location);
        activity.setNotes(notes);
        activity.setCategory(category);
        activity.setEndDate(endDate);
        activity.setOrigin(origin);
        activity.setTransportMode(transportMode);
        activity.setMealType(mealType);
        activity.setBookingRef(bookingRef);
        activity.setBooked(booked);
        activity.setCost(cost);
        activity.setRooms(rooms);
        activity.setGuestsPerRoom(guestsPerRoom);
        activity.setBookingMethod(bookingMethod);
        activity.setContact(contact);
        activity.setParticipantIds(participants);

        // พิกัดต้องมาคู่กันและอยู่ในช่วงที่เป็นไปได้ ไม่มีสถานที่ = ไม่มีพิกัด
        Double lat = request.getLatitude();
        Double lng = request.getLongitude();
        boolean validCoords = lat != null && lng != null && location != null
                && lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
        activity.setLatitude(validCoords ? lat : null);
        activity.setLongitude(validCoords ? lng : null);
    }

    // ตัดซ้ำ คงลำดับ และต้องเป็นสมาชิกของทริปนี้ทุกคน
    private List<Long> cleanParticipants(Long tripId, List<Long> memberIds) {
        if (memberIds == null) {
            return List.of();
        }
        List<Long> result = new ArrayList<>();
        for (Long id : new LinkedHashSet<>(memberIds)) {
            if (id == null) {
                continue;
            }
            TripMember member = tripMemberRepo.findById(id).orElse(null);
            if (member == null || member.getTrip() == null || !tripId.equals(member.getTrip().getId())) {
                throw badRequest("ผู้ร่วมกิจกรรมต้องเป็นสมาชิกในทริปนี้");
            }
            result.add(id);
        }
        return result;
    }

    private static String oneOf(String value, Set<String> allowed) {
        String cleaned = clean(value);
        return cleaned != null && allowed.contains(cleaned) ? cleaned : null;
    }

    private static String cleanBookingRef(String value) {
        String cleaned = clean(value);
        if (cleaned != null && cleaned.length() > MAX_BOOKING_REF_LENGTH) {
            throw badRequest("เลขการจองยาวเกิน " + MAX_BOOKING_REF_LENGTH + " ตัวอักษร");
        }
        return cleaned;
    }

    private Trip requireTrip(Long tripId) {
        if (tripId == null) {
            throw badRequest("กรุณาระบุทริป");
        }
        return tripRepo.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ไม่พบทริปนี้"));
    }

    private Activity requireActivity(Long activityId) {
        return activityRepo.findById(activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ไม่พบกิจกรรมนี้"));
    }

    // เฉพาะสมาชิกของทริปนี้เท่านั้นที่แก้แพลนได้
    private void requireTripMember(Long tripId, Long memberId) {
        if (memberId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "เฉพาะสมาชิกในทริปเท่านั้นที่แก้แพลนได้");
        }
        TripMember member = tripMemberRepo.findById(memberId).orElse(null);
        if (member == null || member.getTrip() == null || !tripId.equals(member.getTrip().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "เฉพาะสมาชิกในทริปเท่านั้นที่แก้แพลนได้");
        }
    }

    private static LocalTime minutes(LocalTime time) {
        return time == null ? null : time.truncatedTo(ChronoUnit.MINUTES);
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
