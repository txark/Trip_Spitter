package com.cp.party_trip.controller;

import com.cp.party_trip.model.ChecklistItem;
import com.cp.party_trip.service.ChecklistService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/checklist")
public class ChecklistController {
    private final ChecklistService checklistService;

    public ChecklistController(ChecklistService checklistService) {
        this.checklistService = checklistService;
    }

    // รองรับการดึงข้อมูลแบบระบุ Filter หมวดหมู่ หรือ ค้นหาชื่อสิ่งของ
    @GetMapping("/{tripId}")
    public ResponseEntity<List<ChecklistItem>> getChecklist(
            @PathVariable Long tripId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(checklistService.getChecklistByTrip(tripId, category, search));
    }

    @PostMapping
    public ResponseEntity<ChecklistItem> addItem(@RequestBody com.cp.party_trip.dto.ChecklistRequestDTO request) {
        ChecklistItem newItem = checklistService.addItem(
                request.getTripId(),
                request.getCategory(),
                request.getItemName(),
                request.getAssignedToMemberId(),
                request.getNotes());
        return ResponseEntity.ok(newItem);
    }

    @PatchMapping("/{itemId}/notes")
    public ResponseEntity<ChecklistItem> updateNotes(
            @PathVariable Long itemId,
            @RequestParam String notes,
            @RequestParam Long memberId) {
        return ResponseEntity.ok(checklistService.updateNotes(itemId, notes, memberId));
    }

    @PatchMapping("/{itemId}/toggle")
    public ResponseEntity<ChecklistItem> toggleCheck(@PathVariable Long itemId) {
        return ResponseEntity.ok(checklistService.toggleCheckStatus(itemId));
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<?> deleteItem(@PathVariable Long itemId) {
        checklistService.deleteItem(itemId);
        return ResponseEntity.ok("ลบรายการสิ่งของสำเร็จ");
    }
}