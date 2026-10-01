package com.cp.party_trip.service;

import com.cp.party_trip.dto.ExpenseRequest;
import com.cp.party_trip.model.Expense;
import com.cp.party_trip.model.ExpenseSplit;
import com.cp.party_trip.model.Trip;
import com.cp.party_trip.model.TripMember;
import com.cp.party_trip.repository.ExpenseRepo;
import com.cp.party_trip.repository.TripMemberRepo;
import com.cp.party_trip.repository.TripRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ExpenseServiceTest {

    private ExpenseRepo expenseRepo;
    private TripMemberRepo tripMemberRepo;
    private ExpenseService service;

    private final Trip trip = new Trip();
    private final Trip otherTrip = new Trip();

    @BeforeEach
    void setUp() {
        expenseRepo = mock(ExpenseRepo.class);
        TripRepo tripRepo = mock(TripRepo.class);
        tripMemberRepo = mock(TripMemberRepo.class);
        service = new ExpenseService(expenseRepo, tripRepo, tripMemberRepo);

        trip.setId(1L);
        otherTrip.setId(2L);
        when(tripRepo.findById(1L)).thenReturn(Optional.of(trip));
        when(expenseRepo.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));

        member(10L, trip);
        member(11L, trip);
        member(12L, trip);
        member(99L, otherTrip);
    }

    private void member(Long id, Trip t) {
        TripMember m = new TripMember();
        m.setId(id);
        m.setTrip(t);
        m.setGuestName("m" + id);
        when(tripMemberRepo.findById(id)).thenReturn(Optional.of(m));
    }

    private ExpenseRequest request(String total, String splitType) {
        ExpenseRequest r = new ExpenseRequest();
        r.setTitle("bill");
        r.setTotalAmount(new BigDecimal(total));
        r.setSplitType(splitType);
        return r;
    }

    private ExpenseRequest.SplitAmount share(Long memberId, String amount) {
        ExpenseRequest.SplitAmount s = new ExpenseRequest.SplitAmount();
        s.setMemberId(memberId);
        s.setAmount(new BigDecimal(amount));
        return s;
    }

    private List<BigDecimal> amounts(Expense e) {
        return e.getExpenseSplits().stream().map(ExpenseSplit::getAmountOwed).toList();
    }

    @Test
    void equalSplitGivesRoundingRemainderToFirstPerson() {
        Expense e = service.createExpense(1L, 10L, request("100", "equal"), List.of(10L, 11L, 12L));

        assertEquals("EQUAL", e.getSplitType());
        assertEquals(List.of(new BigDecimal("33.34"), new BigDecimal("33.33"), new BigDecimal("33.33")), amounts(e));
    }

    @Test
    void customSplitUsesGivenAmountsAndSkipsZeroShares() {
        ExpenseRequest r = request("500", "CUSTOM");
        r.setSplits(List.of(share(10L, "300"), share(11L, "200"), share(12L, "0")));

        Expense e = service.createExpense(1L, 10L, r, null);

        assertEquals("CUSTOM", e.getSplitType());
        assertEquals(List.of(new BigDecimal("300.00"), new BigDecimal("200.00")), amounts(e));
        assertEquals(List.of(10L, 11L),
                e.getExpenseSplits().stream().map(ExpenseSplit::getTripMemberId).toList());
    }

    @Test
    void customSplitRejectsSumThatDoesNotMatchTotal() {
        ExpenseRequest r = request("500", "CUSTOM");
        r.setSplits(List.of(share(10L, "300"), share(11L, "150")));

        assertBadRequest(r);
    }

    @Test
    void customSplitRejectsNegativeAndDuplicateShares() {
        ExpenseRequest negative = request("100", "CUSTOM");
        negative.setSplits(List.of(share(10L, "150"), share(11L, "-50")));
        assertBadRequest(negative);

        ExpenseRequest duplicate = request("100", "CUSTOM");
        duplicate.setSplits(List.of(share(10L, "50"), share(10L, "50")));
        assertBadRequest(duplicate);
    }

    @Test
    void rejectsMemberFromAnotherTrip() {
        ExpenseRequest r = request("100", "CUSTOM");
        r.setSplits(List.of(share(10L, "50"), share(99L, "50")));
        assertBadRequest(r);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.createExpense(1L, 10L, request("100", "EQUAL"), List.of(10L, 99L)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void rejectsNonPositiveTotal() {
        assertBadRequest(request("0", "EQUAL"));
    }

    @Test
    void equalSplitDropsDuplicatesAndFallsBackToPayer() {
        Expense dup = service.createExpense(1L, 10L, request("90", "EQUAL"), List.of(10L, 11L, 11L));
        assertEquals(List.of(new BigDecimal("45.00"), new BigDecimal("45.00")), amounts(dup));

        // ไม่มีผู้ร่วมหาร / ไม่ระบุวิธีหาร = ผู้จ่ายออกเองทั้งหมด (ไม่ใช่บิลที่ไม่มี split)
        Expense solo = service.createExpense(1L, 10L, request("50", null), null);
        assertEquals("EQUAL", solo.getSplitType());
        assertEquals(List.of(10L), solo.getExpenseSplits().stream().map(ExpenseSplit::getTripMemberId).toList());
    }

    @Test
    void rejectsUnknownSplitTypeAndForeignPayer() {
        assertBadRequest(request("100", "PERCENTAGE"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.createExpense(1L, 99L, request("100", "EQUAL"), List.of(10L)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void roundsTotalToTwoDecimals() {
        Expense e = service.createExpense(1L, 10L, request("10.005", "EQUAL"), List.of(10L));
        assertEquals(new BigDecimal("10.01"), e.getTotalAmount());
    }

    @Test
    void rejectsShareWithTooManyDecimals() {
        ExpenseRequest r = request("100.004", "CUSTOM");
        r.setSplits(List.of(share(10L, "50.002"), share(11L, "50.002")));
        assertBadRequest(r);
    }

    private void assertBadRequest(ExpenseRequest r) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.createExpense(1L, 10L, r, List.of(10L)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(expenseRepo, never()).save(any());
    }
}
