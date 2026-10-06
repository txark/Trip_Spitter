package com.cp.party_trip.controller;

import com.cp.party_trip.config.AuthGuard;
import com.cp.party_trip.dto.request.ChecklistBulkRequest;
import com.cp.party_trip.dto.request.ChecklistDetailsRequest;
import com.cp.party_trip.dto.response.ChecklistItemResponse;
import com.cp.party_trip.mapper.ChecklistMapper;
import com.cp.party_trip.service.ChecklistService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/checklist")
public class ChecklistController {
    private final ChecklistService checklistService;
    private final ChecklistMapper checklistMapper;
    private final AuthGuard guard;

    public ChecklistController(ChecklistService checklistService, ChecklistMapper checklistMapper, AuthGuard guard) {
        this.checklistService = checklistService;
        this.checklistMapper = checklistMapper;
        this.guard = guard;
    }

    @GetMapping("/{tripId}")
    public ResponseEntity<List<ChecklistItemResponse>> getChecklist(
            @PathVariable Long tripId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {
        guard.me(tripId);
        return ResponseEntity.ok(checklistMapper.toResponses(checklistService.getChecklistByTrip(tripId, category, search)));
    }

    @PostMapping
    public ResponseEntity<ChecklistItemResponse> addItem(
            @RequestParam Long tripId,
            @RequestParam(required = false) String category,
            @RequestParam String itemName,
            @RequestParam(required = false) Long assignedToMemberId,
            @RequestParam(required = false) String notes) {
        guard.me(tripId);
        return ResponseEntity.status(HttpStatus.CREATED).body(checklistMapper.toResponse(
                checklistService.addItem(tripId, category, itemName, assignedToMemberId, notes)));
    }

    // เพิ่มหลายชิ้นในครั้งเดียว (JSON): หมวด/ผู้รับผิดชอบใช้ร่วมกัน, จำนวน/หน่วย/โน้ตแยกต่อชิ้น
    @PostMapping("/bulk")
    public ResponseEntity<List<ChecklistItemResponse>> addItems(@Valid @RequestBody ChecklistBulkRequest request) {
        guard.me(request.getTripId());
        return ResponseEntity.status(HttpStatus.CREATED).body(checklistMapper.toResponses(checklistService.addItems(
                request.getTripId(), request.getCategory(), request.getItems(), request.getAssigneeIds())));
    }

    // แก้จำนวน/หน่วย/โน้ตของชิ้นนี้ (JSON)
    @PatchMapping("/{itemId}/details")
    public ResponseEntity<ChecklistItemResponse> updateDetails(@PathVariable Long itemId,
            @Valid @RequestBody ChecklistDetailsRequest request) {
        Long me = guard.self(guard.tripOfChecklistItem(itemId), request.getMemberId()).getId();
        return ResponseEntity.ok(checklistMapper.toResponse(checklistService.updateDetails(itemId,
                request.getQuantity(), request.getUnit(), request.getNotes(), me)));
    }

    // ตั้งผู้รับผิดชอบทั้งชุด (JSON array ของ memberId, [] = ยังไม่มีใครรับ)
    @PutMapping("/{itemId}/assignees")
    public ResponseEntity<ChecklistItemResponse> setAssignees(@PathVariable Long itemId,
            @RequestBody List<Long> memberIds) {
        guard.me(guard.tripOfChecklistItem(itemId));
        return ResponseEntity.ok(checklistMapper.toResponse(checklistService.setAssignees(itemId, memberIds)));
    }

    @PatchMapping("/{itemId}/notes")
    public ResponseEntity<ChecklistItemResponse> updateNotes(
            @PathVariable Long itemId,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) Long memberId) {
        Long me = guard.self(guard.tripOfChecklistItem(itemId), memberId).getId();
        return ResponseEntity.ok(checklistMapper.toResponse(checklistService.updateNotes(itemId, notes, me)));
    }

    @PatchMapping("/{itemId}/toggle")
    public ResponseEntity<ChecklistItemResponse> toggleCheck(
            @PathVariable Long itemId,
            @RequestParam(required = false) Long memberId) {
        Long me = guard.self(guard.tripOfChecklistItem(itemId), memberId).getId();
        return ResponseEntity.ok(checklistMapper.toResponse(checklistService.toggleCheckStatus(itemId, me)));
    }

    // เปลี่ยนคนรับผิดชอบ ไม่ส่ง memberId = ยกเลิกคนรับผิดชอบ (มอบให้เพื่อนได้ แต่ต้องอยู่ทริปเดียวกัน)
    @PatchMapping("/{itemId}/assign")
    public ResponseEntity<ChecklistItemResponse> assignItem(
            @PathVariable Long itemId,
            @RequestParam(required = false) Long memberId) {
        guard.me(guard.tripOfChecklistItem(itemId));
        return ResponseEntity.ok(checklistMapper.toResponse(checklistService.assignItem(itemId, memberId)));
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long itemId) {
        guard.me(guard.tripOfChecklistItem(itemId));
        checklistService.deleteItem(itemId);
        return ResponseEntity.noContent().build();
    }
}
