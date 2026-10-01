package com.cp.party_trip.service;

import com.cp.party_trip.dto.ActivityRequest;
import com.cp.party_trip.model.Activity;
import com.cp.party_trip.model.Trip;
import com.cp.party_trip.model.TripMember;
import com.cp.party_trip.repository.ActivityRepo;
import com.cp.party_trip.repository.TripMemberRepo;
import com.cp.party_trip.repository.TripRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ActivityServiceTest {

    private ActivityRepo activityRepo;
    private TripRepo tripRepo;
    private TripMemberRepo tripMemberRepo;
    private ActivityService service;
    private Trip trip;

    @BeforeEach
    void setUp() {
        activityRepo = mock(ActivityRepo.class);
        tripRepo = mock(TripRepo.class);
        tripMemberRepo = mock(TripMemberRepo.class);
        service = new ActivityService(activityRepo, tripRepo, tripMemberRepo);
        when(activityRepo.save(any(Activity.class))).thenAnswer(inv -> inv.getArgument(0));

        trip = new Trip();
        trip.setId(1L);
        trip.setStartDate(LocalDate.of(2026, 10, 12));
        trip.setEndDate(LocalDate.of(2026, 10, 14));
        when(tripRepo.findById(1L)).thenReturn(Optional.of(trip));

        Trip other = new Trip();
        other.setId(2L);
        member(10L, trip);
        member(99L, other);
    }

    private void member(Long id, Trip t) {
        TripMember m = new TripMember();
        m.setId(id);
        m.setTrip(t);
        when(tripMemberRepo.findById(id)).thenReturn(Optional.of(m));
    }

    private ActivityRequest request(String title, LocalDate date) {
        ActivityRequest r = new ActivityRequest();
        r.setTitle(title);
        r.setActivityDate(date);
        r.setMemberId(10L);
        return r;
    }

    private HttpStatus statusOf(Runnable action) {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, action::run);
        return HttpStatus.valueOf(e.getStatusCode().value());
    }

    @Test
    void addActivityCleansFieldsAndKeepsLegacyTimeInSync() {
        ActivityRequest r = request("  คาเฟ่ริมเขา ", LocalDate.of(2026, 10, 13));
        r.setStartTime(LocalTime.of(10, 30, 45));
        r.setEndTime(LocalTime.of(12, 0));
        r.setLocation("  ");
        r.setCategory("UNKNOWN");

        Activity saved = service.addActivity(1L, r);

        assertEquals("คาเฟ่ริมเขา", saved.getTitle());
        assertEquals(LocalTime.of(10, 30), saved.getStartTime());
        assertNull(saved.getLocation());
        assertEquals("OTHER", saved.getCategory());
        assertEquals(10L, saved.getCreatedByMemberId());
        assertEquals(LocalDateTime.of(2026, 10, 13, 10, 30), saved.getActivityTime());
    }

    @Test
    void overnightEndTimeIsAllowed() {
        ActivityRequest r = request("รถทัวร์", LocalDate.of(2026, 10, 12));
        r.setStartTime(LocalTime.of(22, 0));
        r.setEndTime(LocalTime.of(6, 0));
        assertEquals(LocalTime.of(6, 0), service.addActivity(1L, r).getEndTime());
    }

    @Test
    void rejectsDateOutsideTrip() {
        assertEquals(HttpStatus.BAD_REQUEST,
                statusOf(() -> service.addActivity(1L, request("ทะเล", LocalDate.of(2026, 10, 15)))));
        assertEquals(HttpStatus.BAD_REQUEST,
                statusOf(() -> service.addActivity(1L, request("ทะเล", LocalDate.of(2026, 10, 11)))));
        verify(activityRepo, never()).save(any());
    }

    @Test
    void rejectsBadTimesAndBlankTitle() {
        ActivityRequest endOnly = request("ทะเล", LocalDate.of(2026, 10, 12));
        endOnly.setEndTime(LocalTime.of(9, 0));
        assertEquals(HttpStatus.BAD_REQUEST, statusOf(() -> service.addActivity(1L, endOnly)));

        ActivityRequest same = request("ทะเล", LocalDate.of(2026, 10, 12));
        same.setStartTime(LocalTime.of(9, 0));
        same.setEndTime(LocalTime.of(9, 0));
        assertEquals(HttpStatus.BAD_REQUEST, statusOf(() -> service.addActivity(1L, same)));

        assertEquals(HttpStatus.BAD_REQUEST,
                statusOf(() -> service.addActivity(1L, request("   ", LocalDate.of(2026, 10, 12)))));
    }

    @Test
    void onlyTripMembersCanEdit() {
        ActivityRequest outsider = request("ทะเล", LocalDate.of(2026, 10, 12));
        outsider.setMemberId(99L);
        assertEquals(HttpStatus.FORBIDDEN, statusOf(() -> service.addActivity(1L, outsider)));

        ActivityRequest anonymous = request("ทะเล", LocalDate.of(2026, 10, 12));
        anonymous.setMemberId(null);
        assertEquals(HttpStatus.FORBIDDEN, statusOf(() -> service.addActivity(1L, anonymous)));
    }

    @Test
    void missingTripOrActivityIs404() {
        assertEquals(HttpStatus.NOT_FOUND, statusOf(() -> service.getTripActivities(5L)));
        when(activityRepo.findById(7L)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, statusOf(() -> service.deleteActivity(7L, 10L)));
    }

    @Test
    void timelineSortedByDayThenAllDayThenTime() {
        Activity late = activity(1L, LocalDate.of(2026, 10, 12), LocalTime.of(18, 0));
        Activity early = activity(2L, LocalDate.of(2026, 10, 12), LocalTime.of(7, 0));
        Activity allDay = activity(3L, LocalDate.of(2026, 10, 12), null);
        Activity nextDay = activity(4L, LocalDate.of(2026, 10, 13), LocalTime.of(6, 0));
        when(activityRepo.findByTripId(1L)).thenReturn(List.of(nextDay, late, allDay, early));

        List<Long> ids = service.getTripActivities(1L).stream().map(Activity::getId).toList();
        assertEquals(List.of(3L, 2L, 1L, 4L), ids);
    }

    @Test
    void stayCanSpanNightsWithinTrip() {
        ActivityRequest r = request("บ้านไร่", LocalDate.of(2026, 10, 12));
        r.setCategory("STAY");
        r.setStartTime(LocalTime.of(14, 0));
        r.setEndTime(LocalTime.of(12, 0)); // เช็คเอาท์เช้ากว่าเวลาเช็คอินได้ เพราะคนละวัน
        r.setEndDate(LocalDate.of(2026, 10, 14));
        r.setBookingRef(" AG-123 ");
        r.setBookingMethod("ONLINE");
        r.setGuestsPerRoom(2);
        Activity saved = service.addActivity(1L, r);
        assertEquals(LocalDate.of(2026, 10, 14), saved.getEndDate());
        assertEquals("AG-123", saved.getBookingRef());
        assertEquals(Boolean.TRUE, saved.getBooked());
        assertEquals(2, saved.getGuestsPerRoom());

        r.setEndDate(LocalDate.of(2026, 10, 15));
        assertEquals(HttpStatus.BAD_REQUEST, statusOf(() -> service.addActivity(1L, r)));
        r.setEndDate(LocalDate.of(2026, 10, 11));
        assertEquals(HttpStatus.BAD_REQUEST, statusOf(() -> service.addActivity(1L, r)));
        r.setEndDate(LocalDate.of(2026, 10, 12)); // วันเดียวกัน: เช็คเอาท์ต้องหลังเช็คอิน
        assertEquals(HttpStatus.BAD_REQUEST, statusOf(() -> service.addActivity(1L, r)));
    }

    @Test
    void keepsOnlyFieldsOfTheChosenCategory() {
        ActivityRequest r = request("รถตู้ไปเขาใหญ่", LocalDate.of(2026, 10, 12));
        r.setCategory("TRAVEL");
        r.setOrigin("กรุงเทพ");
        r.setTransportMode("VAN");
        r.setMealType("LUNCH");
        r.setEndDate(LocalDate.of(2026, 10, 13));
        r.setCost(new BigDecimal("250"));
        Activity travel = service.addActivity(1L, r);
        assertEquals("กรุงเทพ", travel.getOrigin());
        assertEquals("VAN", travel.getTransportMode());
        assertNull(travel.getMealType());
        assertNull(travel.getEndDate());
        assertEquals(new BigDecimal("250.00"), travel.getCost());

        r.setCategory("FOOD");
        r.setTransportMode("ROCKET");
        Activity food = service.addActivity(1L, r);
        assertEquals("LUNCH", food.getMealType());
        assertNull(food.getOrigin());
        assertNull(food.getTransportMode());
        assertEquals(Boolean.FALSE, food.getBooked());
    }

    @Test
    void travelBookingRefOnlyForModesThatHaveOne() {
        ActivityRequest r = request("ขับรถไปเขาใหญ่", LocalDate.of(2026, 10, 12));
        r.setCategory("TRAVEL");
        r.setTransportMode("CAR");
        r.setBookingRef("12A");
        assertNull(service.addActivity(1L, r).getBookingRef());

        r.setTransportMode("PLANE");
        assertEquals("12A", service.addActivity(1L, r).getBookingRef());
    }

    @Test
    void foodHasOnlyStartTime() {
        ActivityRequest r = request("ส้มตำ", LocalDate.of(2026, 10, 12));
        r.setCategory("FOOD");
        r.setStartTime(LocalTime.of(12, 0));
        r.setEndTime(LocalTime.of(13, 0));
        assertNull(service.addActivity(1L, r).getEndTime());
    }

    @Test
    void stayRoomsMustBeSensible() {
        ActivityRequest r = request("บ้านไร่", LocalDate.of(2026, 10, 12));
        r.setCategory("STAY");
        r.setEndDate(LocalDate.of(2026, 10, 13));
        r.setRooms(2);
        r.setCost(new BigDecimal("1500"));
        assertEquals(2, service.addActivity(1L, r).getRooms());

        r.setRooms(0);
        assertEquals(HttpStatus.BAD_REQUEST, statusOf(() -> service.addActivity(1L, r)));
    }

    @Test
    void activityParticipantsAndBookingRef() {
        ActivityRequest r = request("ล่องแก่ง", LocalDate.of(2026, 10, 12));
        r.setCategory("ACTIVITY");
        r.setParticipantIds(List.of(10L, 10L));
        r.setBookingRef("A102");
        r.setBooked(false);
        Activity notBooked = service.addActivity(1L, r);
        assertEquals(List.of(10L), notBooked.getParticipantIds());
        assertNull(notBooked.getBookingRef()); // ยังไม่จอง = ไม่มีเลขการจอง

        r.setBooked(true);
        assertEquals("A102", service.addActivity(1L, r).getBookingRef());

        r.setParticipantIds(List.of(10L, 99L)); // 99 อยู่คนละทริป
        assertEquals(HttpStatus.BAD_REQUEST, statusOf(() -> service.addActivity(1L, r)));
    }

    @Test
    void coordinatesNeedLocationAndBothValues() {
        ActivityRequest r = request("น้ำตก", LocalDate.of(2026, 10, 12));
        r.setLocation("น้ำตกเหวสุวัต");
        r.setLatitude(14.4);
        r.setLongitude(101.4);
        Activity saved = service.addActivity(1L, r);
        assertEquals(14.4, saved.getLatitude());

        r.setLongitude(null);
        assertNull(service.addActivity(1L, r).getLatitude());
    }

    @Test
    void bookingMethodDecidesBookedAndDetail() {
        ActivityRequest r = request("ส้มตำ", LocalDate.of(2026, 10, 12));
        r.setCategory("FOOD");
        r.setBookingMethod("PHONE");
        r.setBookingRef("081-234-5678");
        Activity phone = service.addActivity(1L, r);
        assertEquals(Boolean.TRUE, phone.getBooked());
        assertEquals("PHONE", phone.getBookingMethod());
        assertEquals("081-234-5678", phone.getBookingRef());

        r.setBookingMethod(null); // ยังไม่จอง = ไม่เก็บรายละเอียดการจอง
        Activity none = service.addActivity(1L, r);
        assertEquals(Boolean.FALSE, none.getBooked());
        assertNull(none.getBookingRef());

        r.setCategory("TRAVEL"); // เดินทางไม่ใช้วิธีจอง
        r.setBookingMethod("PAGE");
        assertNull(service.addActivity(1L, r).getBookingMethod());
    }

    @Test
    void rejectsBadGuestsPerRoom() {
        ActivityRequest r = request("บ้านไร่", LocalDate.of(2026, 10, 12));
        r.setCategory("STAY");
        r.setGuestsPerRoom(0);
        assertEquals(HttpStatus.BAD_REQUEST, statusOf(() -> service.addActivity(1L, r)));
    }

    @Test
    void rejectsBadCost() {
        ActivityRequest r = request("ดำน้ำ", LocalDate.of(2026, 10, 12));
        r.setCost(new BigDecimal("-1"));
        assertEquals(HttpStatus.BAD_REQUEST, statusOf(() -> service.addActivity(1L, r)));
        r.setCost(new BigDecimal("1.005"));
        assertEquals(HttpStatus.BAD_REQUEST, statusOf(() -> service.addActivity(1L, r)));
    }

    private Activity activity(Long id, LocalDate date, LocalTime start) {
        Activity a = new Activity();
        a.setId(id);
        a.setTrip(trip);
        a.setActivityDate(date);
        a.setStartTime(start);
        return a;
    }
}
