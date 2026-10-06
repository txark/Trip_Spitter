package com.cp.party_trip.service.impl;

import com.cp.party_trip.service.ChecklistService;
import com.cp.party_trip.dto.ChecklistBulkRequest;
import com.cp.party_trip.model.ChecklistItem;
import com.cp.party_trip.model.TripMember;
import com.cp.party_trip.repository.ChecklistItemRepo;
import com.cp.party_trip.repository.TripMemberRepo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
public class ChecklistServiceImpl implements ChecklistService {
    static final int MAX_NAME_LENGTH = 100;
    static final int MAX_NOTES_LENGTH = 255;
    static final int MAX_CATEGORY_LENGTH = 30;
    static final int MAX_UNIT_LENGTH = 20;
    static final BigDecimal MAX_QUANTITY = new BigDecimal("99999");
    static final String DEFAULT_CATEGORY = "OTHER";
    static final int MAX_BULK_ITEMS = 50;

    private final ChecklistItemRepo checklistItemRepo;
    private final TripMemberRepo tripMemberRepo;

    public ChecklistServiceImpl(ChecklistItemRepo checklistItemRepo, TripMemberRepo tripMemberRepo) {
        this.checklistItemRepo = checklistItemRepo;
        this.tripMemberRepo = tripMemberRepo;
    }

    @Override
    public List<ChecklistItem> getChecklistByTrip(Long tripId, String category, String search) {
        if (category != null && !category.isEmpty()) {
            return checklistItemRepo.findByTripIdAndCategory(tripId, category);
        }
        if (search != null && !search.isEmpty()) {
            return checklistItemRepo.findByTripIdAndItemNameContainingIgnoreCase(tripId, search);
        }
        return checklistItemRepo.findByTripIdOrderByIdAsc(tripId);
    }

    // เพิ่มทีละชิ้น (endpoint เดิม) — ผู้รับผิดชอบ 1 คนหรือไม่มี
    @Override
    @Transactional
    public ChecklistItem addItem(Long tripId, String category, String itemName, Long assignedToMemberId, String notes) {
        requireTrip(tripId);
        List<Long> assignees = assignedToMemberId == null ? List.of() : List.of(assignedToMemberId);
        return checklistItemRepo.save(newItem(tripId, cleanCategory(category), cleanName(itemName),
                null, null, notes, cleanAssignees(tripId, assignees)));
    }

    // เพิ่มหลายชิ้นพร้อมกัน (ทั้งหมดหรือไม่เลย)
    // - ทุกชิ้นใช้หมวด/ผู้รับผิดชอบชุดเดียวกัน แต่จำนวน/หน่วย/โน้ตแยกต่อชิ้น
    // - ตัดแถวที่ไม่มีชื่อ และชื่อซ้ำในชุดเดียวกัน (ไม่สนตัวพิมพ์) ออก คงลำดับเดิม
    @Override
    @Transactional
    public List<ChecklistItem> addItems(Long tripId, String category, List<ChecklistBulkRequest.NewItem> newItems,
            List<Long> assigneeIds) {
        requireTrip(tripId);
        String cleanCategory = cleanCategory(category);
        List<Long> assignees = cleanAssignees(tripId, assigneeIds);

        List<ChecklistItem> toSave = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ChecklistBulkRequest.NewItem raw : newItems == null ? List.<ChecklistBulkRequest.NewItem>of() : newItems) {
            if (raw == null || raw.getItemName() == null || raw.getItemName().isBlank()) {
                continue;
            }
            String name = cleanName(raw.getItemName());
            if (!seen.add(name.toLowerCase(Locale.ROOT))) {
                continue;
            }
            toSave.add(newItem(tripId, cleanCategory, name, raw.getQuantity(), raw.getUnit(), raw.getNotes(),
                    assignees));
        }
        if (toSave.isEmpty()) {
            throw badRequest("กรุณากรอกชื่อสิ่งของอย่างน้อย 1 ชิ้น");
        }
        if (toSave.size() > MAX_BULK_ITEMS) {
            throw badRequest("เพิ่มได้ครั้งละไม่เกิน " + MAX_BULK_ITEMS + " ชิ้น");
        }
        return checklistItemRepo.saveAll(toSave);
    }

    // แก้จำนวน/หน่วย/โน้ตของชิ้นนี้
    @Override
    @Transactional
    public ChecklistItem updateDetails(Long itemId, BigDecimal quantity, String unit, String notes, Long memberId) {
        ChecklistItem item = findItem(itemId);
        if (memberId != null) {
            requireTripMember(item.getTripId(), memberId);
            item.setUpdatedByMemberId(memberId);
        }
        applyQuantity(item, quantity, unit);
        item.setNotes(cleanNotes(notes));
        return checklistItemRepo.save(item);
    }

    @Override
    @Transactional
    public ChecklistItem updateNotes(Long itemId, String notes, Long memberId) {
        ChecklistItem item = findItem(itemId);
        if (memberId != null) {
            requireTripMember(item.getTripId(), memberId);
        }
        item.setNotes(cleanNotes(notes));
        item.setUpdatedByMemberId(memberId); // บันทึกรหัสสมาชิกที่ทำการอัปเดตล่าสุด
        return checklistItemRepo.save(item);
    }

    // ติ๊ก/ยกเลิกติ๊ก — memberId (ถ้ามี) บันทึกว่าใครเป็นคนกดล่าสุด
    @Override
    @Transactional
    public ChecklistItem toggleCheckStatus(Long itemId, Long memberId) {
        ChecklistItem item = findItem(itemId);
        if (memberId != null) {
            requireTripMember(item.getTripId(), memberId);
            item.setUpdatedByMemberId(memberId);
        }
        item.setChecked(!item.isChecked());
        return checklistItemRepo.save(item);
    }

    // ตั้งผู้รับผิดชอบทั้งชุด (รายการว่าง = ยังไม่มีใครรับ)
    @Override
    @Transactional
    public ChecklistItem setAssignees(Long itemId, List<Long> memberIds) {
        ChecklistItem item = findItem(itemId);
        item.setAssigneeIds(cleanAssignees(item.getTripId(), memberIds));
        return checklistItemRepo.save(item);
    }

    // endpoint เดิม: ผู้รับผิดชอบคนเดียว (memberId = null คือยังไม่มีใครรับ)
    @Override
    @Transactional
    public ChecklistItem assignItem(Long itemId, Long memberId) {
        return setAssignees(itemId, memberId == null ? List.of() : List.of(memberId));
    }

    @Override
    @Transactional
    public void deleteItem(Long itemId) {
        checklistItemRepo.delete(findItem(itemId));
    }

    private ChecklistItem newItem(Long tripId, String category, String name, BigDecimal quantity, String unit,
            String notes, List<Long> assignees) {
        ChecklistItem item = new ChecklistItem();
        item.setTripId(tripId);
        item.setCategory(category);
        item.setItemName(name);
        applyQuantity(item, quantity, unit);
        item.setAssigneeIds(assignees);
        item.setChecked(false);
        item.setNotes(cleanNotes(notes));
        return item;
    }

    // จำนวนต้องมากกว่า 0 (ทศนิยมได้ 2 ตำแหน่ง เช่น 1.5 กก.) ไม่ระบุจำนวน = ไม่เก็บหน่วยด้วย
    private void applyQuantity(ChecklistItem item, BigDecimal quantity, String unit) {
        if (quantity == null) {
            item.setQuantity(null);
            item.setUnit(null);
            return;
        }
        if (quantity.signum() <= 0 || quantity.compareTo(MAX_QUANTITY) > 0) {
            throw badRequest("จำนวนต้องมากกว่า 0 และไม่เกิน " + MAX_QUANTITY.toPlainString());
        }
        if (quantity.stripTrailingZeros().scale() > 2) {
            throw badRequest("จำนวนใส่ทศนิยมได้ไม่เกิน 2 ตำแหน่ง");
        }
        String cleanUnit = unit == null ? "" : unit.trim();
        if (cleanUnit.length() > MAX_UNIT_LENGTH) {
            throw badRequest("หน่วยยาวเกิน " + MAX_UNIT_LENGTH + " ตัวอักษร");
        }
        item.setQuantity(quantity.setScale(2, RoundingMode.HALF_UP));
        item.setUnit(cleanUnit.isEmpty() ? null : cleanUnit);
    }

    // ผู้รับผิดชอบ: ตัดค่าว่าง/ซ้ำ และต้องเป็นสมาชิกของทริปนี้ทุกคน
    private List<Long> cleanAssignees(Long tripId, List<Long> memberIds) {
        List<Long> ids = new ArrayList<>(new LinkedHashSet<>(
                memberIds == null ? List.<Long>of() : memberIds.stream().filter(Objects::nonNull).toList()));
        for (Long id : ids) {
            requireTripMember(tripId, id);
        }
        return ids;
    }

    private void requireTrip(Long tripId) {
        if (tripId == null) {
            throw badRequest("กรุณาระบุทริป");
        }
    }

    private ChecklistItem findItem(Long itemId) {
        return checklistItemRepo.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ไม่พบรายการสิ่งของนี้"));
    }

    private void requireTripMember(Long tripId, Long memberId) {
        TripMember member = tripMemberRepo.findById(memberId)
                .orElseThrow(() -> badRequest("ไม่พบสมาชิกนี้"));
        if (member.getTrip() == null || !tripId.equals(member.getTrip().getId())) {
            throw badRequest("สมาชิกนี้ไม่ได้อยู่ในทริปนี้");
        }
    }

    private String cleanName(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty()) {
            throw badRequest("กรุณากรอกชื่อสิ่งของ");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw badRequest("ชื่อสิ่งของยาวเกิน " + MAX_NAME_LENGTH + " ตัวอักษร");
        }
        return name;
    }

    private String cleanCategory(String raw) {
        String category = raw == null ? "" : raw.trim();
        if (category.isEmpty()) {
            return DEFAULT_CATEGORY;
        }
        if (category.length() > MAX_CATEGORY_LENGTH) {
            throw badRequest("หมวดหมู่ยาวเกิน " + MAX_CATEGORY_LENGTH + " ตัวอักษร");
        }
        return category;
    }

    // โน้ตว่าง = ไม่มีโน้ต (เก็บเป็น null)
    private String cleanNotes(String raw) {
        String notes = raw == null ? "" : raw.trim();
        if (notes.length() > MAX_NOTES_LENGTH) {
            throw badRequest("โน้ตยาวเกิน " + MAX_NOTES_LENGTH + " ตัวอักษร");
        }
        return notes.isEmpty() ? null : notes;
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
