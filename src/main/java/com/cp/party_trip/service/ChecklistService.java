package com.cp.party_trip.service;

import com.cp.party_trip.model.ChecklistItem;
import com.cp.party_trip.repository.ChecklistItemRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChecklistService {
    private final ChecklistItemRepo checklistItemRepo;

    public ChecklistService(ChecklistItemRepo checklistItemRepo) {
        this.checklistItemRepo = checklistItemRepo;
    }

    public List<ChecklistItem> getChecklistByTrip(Long tripId, String category, String search) {
        if (category != null && !category.isEmpty()) {
            return checklistItemRepo.findByTripIdAndCategory(tripId, category);
        }
        if (search != null && !search.isEmpty()) {
            return checklistItemRepo.findByTripIdAndItemNameContainingIgnoreCase(tripId, search);
        }
        return checklistItemRepo.findByTripId(tripId);
    }

    @Transactional
    public ChecklistItem addItem(Long tripId, String category, String itemName, Long assignedToMemberId, String notes) {
        ChecklistItem item = new ChecklistItem();
        item.setTripId(tripId);
        item.setCategory(category);
        item.setItemName(itemName);
        item.setAssignedToMemberId(assignedToMemberId);
        item.setChecked(false);
        item.setNotes(notes);
        return checklistItemRepo.save(item);
    }

    @Transactional
    public ChecklistItem updateNotes(Long itemId, String notes) {
        ChecklistItem item = checklistItemRepo.findById(itemId)
                .orElseThrow(() -> new RuntimeException("ไม่พบรายการสิ่งของนี้"));
        item.setNotes(notes);
        return checklistItemRepo.save(item);
    }

    @Transactional
    public ChecklistItem toggleCheckStatus(Long itemId) {
        ChecklistItem item = checklistItemRepo.findById(itemId)
                .orElseThrow(() -> new RuntimeException("ไม่พบรายการสิ่งของนี้"));
        item.setChecked(!item.isChecked());
        return checklistItemRepo.save(item);
    }

    @Transactional
    public void deleteItem(Long itemId) {
        checklistItemRepo.deleteById(itemId);
    }

    @Transactional
    public ChecklistItem updateNotes(Long itemId, String notes, Long memberId) {
        ChecklistItem item = checklistItemRepo.findById(itemId)
                .orElseThrow(() -> new RuntimeException("ไม่พบรายการสิ่งของนี้"));
        item.setNotes(notes);
        item.setUpdatedByMemberId(memberId); // บันทึกรหัสสมาชิกที่ทำการอัปเดตล่าสุด
        return checklistItemRepo.save(item);
    }
}