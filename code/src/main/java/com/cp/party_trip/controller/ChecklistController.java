package com.cp.party_trip.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Checklist - รายการสิ่งของ")
@RestController
@RequestMapping("/api/v1")
public class ChecklistController {
    private final ChecklistService checklistService;
    private final ChecklistMapper checklistMapper;
    private final AuthGuard guard;

    public ChecklistController(ChecklistService checklistService, ChecklistMapper checklistMapper, AuthGuard guard) {
        this.checklistService = checklistService;
        this.checklistMapper = checklistMapper;
        this.guard = guard;
    }

    @GetMapping("/trips/{tripId}/checklist-items")
    public ResponseEntity<List<ChecklistItemResponse>> getChecklist(
            @PathVariable Long tripId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {
        guard.me(tripId);
        return ResponseEntity.ok(checklistMapper.toResponses(checklistService.getChecklistByTrip(tripId, category, search)));
    }

    @PostMapping("/trips/{tripId}/checklist-items")
    public ResponseEntity<ChecklistItemResponse> addItem(
            @PathVariable Long tripId,
            @RequestParam(required = false) String category,
            @RequestParam String itemName,
            @RequestParam(required = false) Long assignedToMemberId,
            @RequestParam(required = false) String notes) {
        guard.me(tripId);
        return ResponseEntity.status(HttpStatus.CREATED).body(checklistMapper.toResponse(
                checklistService.addItem(tripId, category, itemName, assignedToMemberId, notes)));
    }

    // เพิ่มหลายชิ้นในครั้งเดียว (JSON): หมวด/ผู้รับผิดชอบใช้ร่วมกัน, จำนวน/หน่วย/โน้ตแยกต่อชิ้น
    @PostMapping("/trips/{tripId}/checklist-items/bulk")
    public ResponseEntity<List<ChecklistItemResponse>> addItems(@PathVariable Long tripId,
            @Valid @RequestBody ChecklistBulkRequest request) {
        guard.me(tripId);
        return ResponseEntity.status(HttpStatus.CREATED).body(checklistMapper.toResponses(checklistService.addItems(
                tripId, request.getCategory(), request.getItems(), request.getAssigneeIds())));
    }

    // แก้จำนวน/หน่วย/โน้ตของชิ้นนี้ (JSON)
    @PatchMapping("/checklist-items/{itemId}")
    public ResponseEntity<ChecklistItemResponse> updateDetails(@PathVariable Long itemId,
            @Valid @RequestBody ChecklistDetailsRequest request) {
        Long me = guard.self(guard.tripOfChecklistItem(itemId), request.getMemberId()).getId();
        return ResponseEntity.ok(checklistMapper.toResponse(checklistService.updateDetails(itemId,
                request.getQuantity(), request.getUnit(), request.getNotes(), me)));
    }

    // ตั้งผู้รับผิดชอบทั้งชุด (JSON array ของ memberId, [] = ยังไม่มีใครรับ)
    @PutMapping("/checklist-items/{itemId}/assignees")
    public ResponseEntity<ChecklistItemResponse> setAssignees(@PathVariable Long itemId,
            @RequestBody List<Long> memberIds) {
        guard.me(guard.tripOfChecklistItem(itemId));
        return ResponseEntity.ok(checklistMapper.toResponse(checklistService.setAssignees(itemId, memberIds)));
    }

    @PatchMapping("/checklist-items/{itemId}/notes")
    public ResponseEntity<ChecklistItemResponse> updateNotes(
            @PathVariable Long itemId,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) Long memberId) {
        Long me = guard.self(guard.tripOfChecklistItem(itemId), memberId).getId();
        return ResponseEntity.ok(checklistMapper.toResponse(checklistService.updateNotes(itemId, notes, me)));
    }

    @PatchMapping("/checklist-items/{itemId}/check-status")
    public ResponseEntity<ChecklistItemResponse> toggleCheck(
            @PathVariable Long itemId,
            @RequestParam(required = false) Long memberId) {
        Long me = guard.self(guard.tripOfChecklistItem(itemId), memberId).getId();
        return ResponseEntity.ok(checklistMapper.toResponse(checklistService.toggleCheckStatus(itemId, me)));
    }

    // เปลี่ยนคนรับผิดชอบ ไม่ส่ง memberId = ยกเลิกคนรับผิดชอบ (มอบให้เพื่อนได้ แต่ต้องอยู่ทริปเดียวกัน)
    @PutMapping("/checklist-items/{itemId}/assignee")
    public ResponseEntity<ChecklistItemResponse> assignItem(
            @PathVariable Long itemId,
            @RequestParam(required = false) Long memberId) {
        guard.me(guard.tripOfChecklistItem(itemId));
        return ResponseEntity.ok(checklistMapper.toResponse(checklistService.assignItem(itemId, memberId)));
    }

    @DeleteMapping("/checklist-items/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long itemId) {
        guard.me(guard.tripOfChecklistItem(itemId));
        checklistService.deleteItem(itemId);
        return ResponseEntity.noContent().build();
    }
}
