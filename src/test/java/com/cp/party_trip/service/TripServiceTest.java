package com.cp.party_trip.service;

import com.cp.party_trip.model.Trip;
import com.cp.party_trip.model.TripMember;
import com.cp.party_trip.repository.TripMemberRepo;
import com.cp.party_trip.repository.TripRepo;
import com.cp.party_trip.repository.UserRepo;
import com.cp.party_trip.repository.UserTripHistoryRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TripServiceTest {

    private TripRepo tripRepo;
    private TripService service;
    private final Trip trip = new Trip();

    @BeforeEach
    void setUp() {
        tripRepo = mock(TripRepo.class);
        TripMemberRepo tripMemberRepo = mock(TripMemberRepo.class);
        service = new TripService(tripRepo, tripMemberRepo, mock(UserRepo.class), mock(UserTripHistoryRepo.class));
        when(tripRepo.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));

        trip.setId(1L);
        when(tripRepo.findById(1L)).thenReturn(Optional.of(trip));
        Trip other = new Trip();
        other.setId(2L);

        TripMember mine = new TripMember();
        mine.setId(10L);
        mine.setTrip(trip);
        when(tripMemberRepo.findById(10L)).thenReturn(Optional.of(mine));
        TripMember stranger = new TripMember();
        stranger.setId(99L);
        stranger.setTrip(other);
        when(tripMemberRepo.findById(99L)).thenReturn(Optional.of(stranger));
    }

    private HttpStatus statusOf(Runnable action) {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, action::run);
        return HttpStatus.valueOf(e.getStatusCode().value());
    }

    @Test
    void memberCanSetTripTimeZone() {
        assertEquals("Asia/Tokyo", service.updateTimeZone(1L, 10L, " Asia/Tokyo ").getTimeZone());
    }

    @Test
    void rejectsInvalidOrFixedOffsetZones() {
        assertEquals(HttpStatus.BAD_REQUEST, statusOf(() -> service.updateTimeZone(1L, 10L, "Mars/Olympus")));
        assertEquals(HttpStatus.BAD_REQUEST, statusOf(() -> service.updateTimeZone(1L, 10L, "+07:00")));
        assertEquals(HttpStatus.BAD_REQUEST, statusOf(() -> service.updateTimeZone(1L, 10L, null)));
        verify(tripRepo, never()).save(any());
        assertNull(trip.getTimeZone());
    }

    @Test
    void onlyTripMembersCanChangeTimeZone() {
        assertEquals(HttpStatus.FORBIDDEN, statusOf(() -> service.updateTimeZone(1L, 99L, "Asia/Tokyo")));
        assertEquals(HttpStatus.FORBIDDEN, statusOf(() -> service.updateTimeZone(1L, null, "Asia/Tokyo")));
        verify(tripRepo, never()).save(any());
    }

    @Test
    void memberCanSetAndClearBudget() {
        assertEquals(new java.math.BigDecimal("8000.50"),
                service.updateBudget(1L, 10L, new java.math.BigDecimal("8000.5")).getBudgetPerPerson());
        assertNull(service.updateBudget(1L, 10L, java.math.BigDecimal.ZERO).getBudgetPerPerson());
        service.updateBudget(1L, 10L, new java.math.BigDecimal("100"));
        assertNull(service.updateBudget(1L, 10L, null).getBudgetPerPerson());
    }

    @Test
    void rejectsInvalidBudgetAndNonMembers() {
        assertEquals(HttpStatus.BAD_REQUEST, statusOf(() -> service.updateBudget(1L, 10L, new java.math.BigDecimal("-1"))));
        assertEquals(HttpStatus.BAD_REQUEST, statusOf(() -> service.updateBudget(1L, 10L, new java.math.BigDecimal("10000000"))));
        assertEquals(HttpStatus.BAD_REQUEST, statusOf(() -> service.updateBudget(1L, 10L, new java.math.BigDecimal("1.005"))));
        assertEquals(HttpStatus.FORBIDDEN, statusOf(() -> service.updateBudget(1L, 99L, new java.math.BigDecimal("100"))));
        verify(tripRepo, never()).save(any());
    }

    @Test
    void memberCanSetAndClearTripCurrency() {
        Trip saved = service.updateCurrency(1L, 10L, "jpy", new java.math.BigDecimal("0.2283"));
        assertEquals("JPY", saved.getCurrency());
        assertEquals(new java.math.BigDecimal("0.228300"), saved.getExchangeRate());
        Trip cleared = service.updateCurrency(1L, 10L, "THB", null);
        assertNull(cleared.getCurrency());
        assertNull(cleared.getExchangeRate());
    }

    @Test
    void rejectsBadCurrencyOrRate() {
        assertEquals(HttpStatus.BAD_REQUEST, statusOf(() -> service.updateCurrency(1L, 10L, "YEN!", new java.math.BigDecimal("1"))));
        assertEquals(HttpStatus.BAD_REQUEST, statusOf(() -> service.updateCurrency(1L, 10L, "JPY", null)));
        assertEquals(HttpStatus.BAD_REQUEST, statusOf(() -> service.updateCurrency(1L, 10L, "JPY", java.math.BigDecimal.ZERO)));
        assertEquals(HttpStatus.FORBIDDEN, statusOf(() -> service.updateCurrency(1L, 99L, "JPY", java.math.BigDecimal.ONE)));
        verify(tripRepo, never()).save(any());
    }

    @Test
    void missingTripIsNotFound() {
        assertEquals(HttpStatus.NOT_FOUND, statusOf(() -> service.updateTimeZone(5L, 10L, "Asia/Tokyo")));
    }
}
