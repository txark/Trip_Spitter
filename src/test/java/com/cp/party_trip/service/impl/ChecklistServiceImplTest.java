package com.cp.party_trip.service.impl;

import com.cp.party_trip.dto.ChecklistBulkRequest;
import com.cp.party_trip.model.ChecklistItem;
import com.cp.party_trip.model.Trip;
import com.cp.party_trip.model.TripMember;
import com.cp.party_trip.repository.ChecklistItemRepo;
import com.cp.party_trip.repository.TripMemberRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class ChecklistServiceImplTest {

    private ChecklistItemRepo itemRepo;
    private TripMemberRepo tripMemberRepo;
    private ChecklistServiceImpl service;

    @BeforeEach
    void setUp() {
        itemRepo = mock(ChecklistItemRepo.class);
        tripMemberRepo = mock(TripMemberRepo.class);
        service = new ChecklistServiceImpl(itemRepo, tripMemberRepo);
        when(itemRepo.save(any(ChecklistItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(itemRepo.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        Trip trip = new Trip();
        trip.setId(1L);
        Trip other = new Trip();
        other.setId(2L);
        member(10L, trip);
        member(11L, trip);
        member(99L, other);
    }

    private void member(Long id, Trip t) {
        TripMember m = new TripMember();
        m.setId(id);
        m.setTrip(t);
        m.setGuestName("m" + id);
        when(tripMemberRepo.findById(id)).thenReturn(Optional.of(m));
    }

    private ChecklistItem item(boolean checked) {
        ChecklistItem i = new ChecklistItem();
        i.setId(7L);
        i.setTripId(1L);
        i.setItemName("ครีมกันแดด");
        i.setChecked(checked);
        when(itemRepo.findById(7L)).thenReturn(Optional.of(i));
        return i;
    }

    private void assertStatus(HttpStatus status, Runnable action) {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, action::run);
        assertEquals(status, e.getStatusCode());
    }

    @Test
    void addItemTrimsAndDefaultsCategory() {
        ChecklistItem saved = service.addItem(1L, " ", "  ครีมกันแดด  ", 10L, "   ");

        assertEquals("ครีมกันแดด", saved.getItemName());
        assertEquals("OTHER", saved.getCategory());
        assertNull(saved.getNotes());
        assertFalse(saved.isChecked());
    }

    @Test
    void addItemRejectsBlankNameAndOutsideMember() {
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.addItem(1L, "GEAR", "  ", null, null));
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.addItem(1L, "GEAR", "เต็นท์", 99L, null));
        verify(itemRepo, never()).save(any());
    }

    private ChecklistBulkRequest.NewItem row(String name, String qty, String unit, String notes) {
        ChecklistBulkRequest.NewItem r = new ChecklistBulkRequest.NewItem();
        r.setItemName(name);
        r.setQuantity(qty == null ? null : new BigDecimal(qty));
        r.setUnit(unit);
        r.setNotes(notes);
        return r;
    }

    @Test
    void addItemsKeepsPerItemDetailsAndSharedAssignees() {
        List<ChecklistItem> saved = service.addItems(1L, "GEAR", List.of(
                row(" เต็นท์ ", "2", " หลัง ", "ของพี่บี"),
                row("", "1", null, null),
                row("ไฟฉาย", null, "ชิ้น", "  "),
                row("เต็นท์", "5", null, null)), List.of(10L, 11L, 10L));

        assertEquals(List.of("เต็นท์", "ไฟฉาย"), saved.stream().map(ChecklistItem::getItemName).toList());
        ChecklistItem tent = saved.get(0);
        assertEquals(new BigDecimal("2.00"), tent.getQuantity());
        assertEquals("หลัง", tent.getUnit());
        assertEquals("ของพี่บี", tent.getNotes());
        // ไม่ระบุจำนวน = ไม่เก็บหน่วย, โน้ตว่าง = null
        assertNull(saved.get(1).getQuantity());
        assertNull(saved.get(1).getUnit());
        assertNull(saved.get(1).getNotes());
        // ผู้รับผิดชอบซ้ำถูกตัดออก ทุกชิ้นได้ชุดเดียวกัน
        assertTrue(saved.stream().allMatch(i -> i.getAssigneeIds().equals(List.of(10L, 11L))));
        assertEquals(10L, tent.getAssignedToMemberId());
    }

    @Test
    void addItemsRejectsEmptyTooManyBadQuantityAndOutsider() {
        assertStatus(HttpStatus.BAD_REQUEST,
                () -> service.addItems(1L, null, List.of(row(" ", null, null, null)), List.of(10L)));

        List<ChecklistBulkRequest.NewItem> many = java.util.stream.IntStream.rangeClosed(1, 51)
                .mapToObj(i -> row("ของ " + i, null, null, null)).toList();
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.addItems(1L, null, many, List.of(10L)));

        assertStatus(HttpStatus.BAD_REQUEST,
                () -> service.addItems(1L, null, List.of(row("น้ำ", "0", "ขวด", null)), List.of(10L)));
        assertStatus(HttpStatus.BAD_REQUEST,
                () -> service.addItems(1L, null, List.of(row("น้ำ", "1.555", "ลิตร", null)), List.of(10L)));
        assertStatus(HttpStatus.BAD_REQUEST,
                () -> service.addItems(1L, null, List.of(row("น้ำ", null, null, null)), List.of(10L, 99L)));
        verify(itemRepo, never()).saveAll(anyList());
    }

    @Test
    void setAssigneesAndUpdateDetails() {
        item(false);

        assertEquals(List.of(11L, 10L), service.setAssignees(7L, List.of(11L, 10L, 11L)).getAssigneeIds());
        assertEquals(List.of(), service.setAssignees(7L, List.of()).getAssigneeIds());
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.setAssignees(7L, List.of(99L)));

        ChecklistItem updated = service.updateDetails(7L, new BigDecimal("1.5"), "กก.", " SPF50 ", 10L);
        assertEquals(new BigDecimal("1.50"), updated.getQuantity());
        assertEquals("กก.", updated.getUnit());
        assertEquals("SPF50", updated.getNotes());

        ChecklistItem cleared = service.updateDetails(7L, null, "กก.", "", 10L);
        assertNull(cleared.getQuantity());
        assertNull(cleared.getUnit());
        assertNull(cleared.getNotes());
    }

    @Test
    void legacySingleAssigneeIsReadAsList() {
        ChecklistItem legacy = new ChecklistItem();
        legacy.setAssignedToMemberId(10L);
        assertEquals(List.of(10L), legacy.getAssigneeIds());
    }

    @Test
    void toggleFlipsAndRecordsMember() {
        ChecklistItem i = item(false);

        assertTrue(service.toggleCheckStatus(7L, 10L).isChecked());
        assertEquals(10L, i.getUpdatedByMemberId());
        assertFalse(service.toggleCheckStatus(7L, null).isChecked());
    }

    @Test
    void assignAndUnassign() {
        item(false);

        assertEquals(10L, service.assignItem(7L, 10L).getAssignedToMemberId());
        assertNull(service.assignItem(7L, null).getAssignedToMemberId());
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.assignItem(7L, 99L));
    }

    @Test
    void missingItemIsNotFound() {
        when(itemRepo.findById(8L)).thenReturn(Optional.empty());
        assertStatus(HttpStatus.NOT_FOUND, () -> service.toggleCheckStatus(8L, null));
        assertStatus(HttpStatus.NOT_FOUND, () -> service.deleteItem(8L));
    }
}
