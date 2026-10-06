package com.cp.party_trip.service.impl;

import com.cp.party_trip.dto.request.ExpenseRequest;
import com.cp.party_trip.model.Expense;
import com.cp.party_trip.model.ExpenseSplit;
import com.cp.party_trip.model.Trip;
import com.cp.party_trip.model.TripMember;
import com.cp.party_trip.model.Activity;
import com.cp.party_trip.repository.ActivityRepo;
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

class ExpenseServiceImplTest {

    private ExpenseRepo expenseRepo;
    private ActivityRepo activityRepo;
    private TripMemberRepo tripMemberRepo;
    private ExpenseServiceImpl service;

    private final Trip trip = new Trip();
    private final Trip otherTrip = new Trip();

    @BeforeEach
    void setUp() {
        expenseRepo = mock(ExpenseRepo.class);
        TripRepo tripRepo = mock(TripRepo.class);
        tripMemberRepo = mock(TripMemberRepo.class);
        activityRepo = mock(ActivityRepo.class);
        service = new ExpenseServiceImpl(expenseRepo, tripRepo, tripMemberRepo, activityRepo,
                mock(com.cp.party_trip.repository.ExpenseSplitRepo.class),
                new com.cp.party_trip.mapper.ExpenseMapper(new com.cp.party_trip.mapper.MemberMapper()));

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
    void linksBillToPlanActivityOfSameTripOnly() {
        Activity mine = new Activity();
        mine.setId(5L);
        mine.setTrip(trip);
        when(activityRepo.findById(5L)).thenReturn(Optional.of(mine));

        ExpenseRequest r = request("300", "EQUAL");
        r.setActivityId(5L);
        assertEquals(5L, service.createExpense(1L, 10L, r, List.of(10L, 11L)).getActivityId());
    }

    @Test
    void rejectsPlanActivityFromAnotherTripOrMissing() {
        Activity foreign = new Activity();
        foreign.setId(6L);
        foreign.setTrip(otherTrip);
        when(activityRepo.findById(6L)).thenReturn(Optional.of(foreign));

        ExpenseRequest r = request("300", "EQUAL");
        r.setActivityId(6L); // รายการของทริปอื่น
        assertBadRequest(r);
        r.setActivityId(7L); // ไม่มีรายการนี้
        assertBadRequest(r);
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

    @Test
    void foreignCurrencyBillKeepsReceiptAmountAndRate() {
        ExpenseRequest r = request("690", "EQUAL");
        r.setCurrency("jpy");
        r.setOriginalAmount(new BigDecimal("3000"));
        r.setExchangeRate(new BigDecimal("0.23"));
        Expense e = service.createExpense(1L, 10L, r, List.of(10L, 11L));
        assertEquals("JPY", e.getCurrency());
        assertEquals(new BigDecimal("3000.00"), e.getOriginalAmount());
        assertEquals(new BigDecimal("0.230000"), e.getExchangeRate());
        assertEquals(List.of(new BigDecimal("345.00"), new BigDecimal("345.00")), amounts(e)); // หารเป็นบาท
    }

    @Test
    void foreignBillMustMatchRateAndHaveReceiptAmount() {
        ExpenseRequest wrong = request("700", "EQUAL");
        wrong.setCurrency("JPY");
        wrong.setOriginalAmount(new BigDecimal("3000"));
        wrong.setExchangeRate(new BigDecimal("0.23"));
        assertBadRequest(wrong);

        ExpenseRequest noRate = request("690", "EQUAL");
        noRate.setCurrency("JPY");
        noRate.setOriginalAmount(new BigDecimal("3000"));
        assertBadRequest(noRate);
    }

    @Test
    void bahtBillHasNoForeignFields() {
        ExpenseRequest r = request("100", "EQUAL");
        r.setCurrency("THB");
        r.setOriginalAmount(new BigDecimal("5"));
        r.setExchangeRate(new BigDecimal("20"));
        Expense e = service.createExpense(1L, 10L, r, List.of(10L));
        assertEquals("THB", e.getCurrency());
        assertNull(e.getOriginalAmount());
        assertNull(e.getExchangeRate());
    }

    // บิลที่บันทึกไว้แล้ว: คนจ่าย 10 หารกับ 11 คนละ 50 (paidFriend = 11 จ่ายคืนแล้ว)
    private Expense existingBill(boolean paidFriend, boolean paidPayer) {
        Expense e = new Expense();
        e.setId(40L);
        e.setTrip(trip);
        e.setTitle("old");
        e.setTotalAmount(new BigDecimal("100.00"));
        e.setActivityId(5L);
        e.setUser(tripMemberRepo.findById(10L).orElseThrow());
        ExpenseSplit mine = new ExpenseSplit();
        mine.setTripMember(e.getUser());
        mine.setAmountOwed(new BigDecimal("50.00"));
        mine.setPaid(paidPayer);
        ExpenseSplit friend = new ExpenseSplit();
        friend.setTripMember(tripMemberRepo.findById(11L).orElseThrow());
        friend.setAmountOwed(new BigDecimal("50.00"));
        friend.setPaid(paidFriend);
        e.setExpenseSplits(new java.util.ArrayList<>(List.of(mine, friend)));
        when(expenseRepo.lockById(40L)).thenReturn(Optional.of(e));
        return e;
    }

    @Test
    void payerCanEditBillAndSplitsAreRebuilt() {
        Expense e = existingBill(false, true); // ส่วนของคนจ่ายเองไม่นับว่า "จ่ายคืน"
        List<ExpenseSplit> sameList = e.getExpenseSplits();

        ExpenseRequest r = request("300", "EQUAL");
        r.setTitle(" new ");
        Expense saved = service.updateExpense(40L, 10L, r, List.of(10L, 11L, 12L));

        assertEquals("new", saved.getTitle());
        assertEquals(new BigDecimal("300.00"), saved.getTotalAmount());
        assertEquals(List.of(new BigDecimal("100.00"), new BigDecimal("100.00"), new BigDecimal("100.00")),
                amounts(saved));
        assertSame(sameList, saved.getExpenseSplits()); // ลิสต์เดิม ให้ orphanRemoval ลบแถวเก่า
        assertNull(saved.getActivityId()); // ไม่ส่ง activityId มา = เลิกผูกกับแพลน
        assertEquals(10L, saved.getUser().getId());
    }

    @Test
    void onlyPayerCanEditOrDelete() {
        existingBill(false, false);
        assertStatus(HttpStatus.FORBIDDEN, () -> service.updateExpense(40L, 11L, request("10", "EQUAL"), null));
        assertStatus(HttpStatus.FORBIDDEN, () -> service.deleteExpense(40L, null));
        verify(expenseRepo, never()).save(any());
        verify(expenseRepo, never()).delete(any());
    }

    @Test
    void billIsLockedOnceAFriendHasPaidBack() {
        Expense e = existingBill(true, false);
        assertStatus(HttpStatus.CONFLICT, () -> service.updateExpense(40L, 10L, request("10", "EQUAL"), null));
        assertStatus(HttpStatus.CONFLICT, () -> service.deleteExpense(40L, 10L));
        assertEquals(2, e.getExpenseSplits().size());
        verify(expenseRepo, never()).save(any());
        verify(expenseRepo, never()).delete(any());
    }

    @Test
    void editValidatesLikeCreate() {
        existingBill(false, false);
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.updateExpense(40L, 10L, request("0", "EQUAL"), null));
        assertStatus(HttpStatus.BAD_REQUEST,
                () -> service.updateExpense(40L, 10L, request("100", "EQUAL"), List.of(10L, 99L)));
        verify(expenseRepo, never()).save(any());
    }

    @Test
    void payerCanDeleteBill() {
        Expense e = existingBill(false, false);
        service.deleteExpense(40L, 10L);
        verify(expenseRepo).delete(e);
    }

    @Test
    void canRecordBillPaidByFriendAndRecorderCanEditIt() {
        Expense e = service.createExpense(1L, 11L, 10L, request("100", "EQUAL"), List.of(11L, 10L));
        assertEquals(11L, e.getUser().getId());
        assertEquals(10L, e.getRecordedById());

        e.setId(40L);
        when(expenseRepo.lockById(40L)).thenReturn(Optional.of(e));
        service.updateExpense(40L, 10L, request("60", "EQUAL"), List.of(11L, 10L)); // คนบันทึกแก้ได้
        service.updateExpense(40L, 11L, request("80", "EQUAL"), List.of(11L, 10L)); // คนจ่ายแก้ได้
        assertStatus(HttpStatus.FORBIDDEN, () -> service.updateExpense(40L, 12L, request("10", "EQUAL"), null));
        assertStatus(HttpStatus.FORBIDDEN, () -> service.deleteExpense(40L, 12L));
    }

    @Test
    void ownBillIsRecordedByPayer() {
        assertEquals(10L, service.createExpense(1L, 10L, request("50", "EQUAL"), null).getRecordedById());
        assertStatus(HttpStatus.BAD_REQUEST,
                () -> service.createExpense(1L, 10L, 99L, request("50", "EQUAL"), null)); // คนบันทึกอยู่ทริปอื่น
    }

    @Test
    void editCanChangePayerAndEditorKeepsAccess() {
        existingBill(false, false);
        Expense saved = service.updateExpense(40L, 10L, 12L, request("100", "EQUAL"), List.of(12L, 10L));
        assertEquals(12L, saved.getUser().getId());
        assertEquals(10L, saved.getRecordedById());
        assertStatus(HttpStatus.BAD_REQUEST,
                () -> service.updateExpense(40L, 10L, 99L, request("100", "EQUAL"), null));
    }

    @Test
    void billDateCanBeChosenAndEditedKeepingTime() {
        ExpenseRequest r = request("100", "EQUAL");
        r.setExpenseDate(java.time.LocalDate.of(2026, 10, 1));
        Expense e = service.createExpense(1L, 10L, r, null);
        assertEquals(java.time.LocalDate.of(2026, 10, 1), e.getExpenseDate().toLocalDate());

        Expense old = existingBill(false, false);
        old.setExpenseDate(java.time.LocalDateTime.of(2026, 10, 3, 14, 30));
        ExpenseRequest edit = request("100", "EQUAL");
        edit.setExpenseDate(java.time.LocalDate.of(2026, 10, 2));
        assertEquals(java.time.LocalDateTime.of(2026, 10, 2, 14, 30),
                service.updateExpense(40L, 10L, edit, null).getExpenseDate());

        ExpenseRequest noDate = request("100", "EQUAL");
        assertEquals(java.time.LocalDate.of(2026, 10, 2),
                service.updateExpense(40L, 10L, noDate, null).getExpenseDate().toLocalDate()); // ไม่ส่ง = วันเดิม

        ExpenseRequest bad = request("100", "EQUAL");
        bad.setExpenseDate(java.time.LocalDate.of(1900, 1, 1));
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.updateExpense(40L, 10L, bad, null));

        ExpenseRequest future = request("100", "EQUAL");
        future.setExpenseDate(java.time.LocalDate.now().plusDays(2));
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.createExpense(1L, 10L, future, null));
        ExpenseRequest tomorrow = request("100", "EQUAL");
        tomorrow.setExpenseDate(java.time.LocalDate.now().plusDays(1)); // เผื่อเขตเวลาต่างกัน
        assertEquals(java.time.LocalDate.now().plusDays(1),
                service.createExpense(1L, 10L, tomorrow, null).getExpenseDate().toLocalDate());
    }

    @Test
    void editFromStaleFormIsRejectedAndRevisionGoesUp() {
        Expense e = existingBill(false, false);
        assertEquals(0, e.getRevision());
        service.updateExpense(40L, 10L, null, request("100", "EQUAL"), List.of(10L, 11L), 0);
        assertEquals(1, e.getRevision());
        // อีกเครื่องเปิดฟอร์มไว้ตอนรุ่น 0 แล้วกดบันทึกทีหลัง
        assertStatus(HttpStatus.CONFLICT,
                () -> service.updateExpense(40L, 10L, null, request("200", "EQUAL"), List.of(10L, 11L), 0));
        assertEquals(new java.math.BigDecimal("100.00"), e.getTotalAmount());
        assertStatus(HttpStatus.CONFLICT, () -> service.deleteExpense(40L, 10L, 0));
        service.deleteExpense(40L, 10L, 1);
        verify(expenseRepo).delete(e);
    }

    @Test
    void editingMissingBillIsNotFound() {
        assertStatus(HttpStatus.NOT_FOUND, () -> service.updateExpense(41L, 10L, request("10", "EQUAL"), null));
        assertStatus(HttpStatus.NOT_FOUND, () -> service.deleteExpense(41L, 10L));
    }

    private void assertStatus(HttpStatus status, org.junit.jupiter.api.function.Executable call) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, call);
        assertEquals(status, ex.getStatusCode());
    }

    private void assertBadRequest(ExpenseRequest r) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.createExpense(1L, 10L, r, List.of(10L)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(expenseRepo, never()).save(any());
    }
}
