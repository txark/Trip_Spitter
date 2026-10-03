package com.cp.party_trip.controller;

import com.cp.party_trip.config.AuthGuard;
import com.cp.party_trip.dto.ChecklistBulkRequest;
import com.cp.party_trip.dto.ChecklistDetailsRequest;
import com.cp.party_trip.model.ChecklistItem;
import com.cp.party_trip.service.ChecklistService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/checklist")
public class ChecklistController {
    private final ChecklistService checklistService;
    private final AuthGuard guard;

    public ChecklistController(ChecklistService checklistService, AuthGuard guard) {
        this.checklistService = checklistService;
        this.guard = guard;
    }

    @GetMapping("/{tripId}")
    public ResponseEntity<List<ChecklistItem>> getChecklist(
            @PathVariable Long tripId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {
        guard.me(tripId);
        return ResponseEntity.ok(checklistService.getChecklistByTrip(tripId, category, search));
    }

    @PostMapping
    public ResponseEntity<?> addItem(
            @RequestParam Long tripId,
            @RequestParam(required = false) String category,
            @RequestParam String itemName,
            @RequestParam(required = false) Long assignedToMemberId,
            @RequestParam(required = false) String notes) {
        try {
            guard.me(tripId);
            return ResponseEntity.ok(checklistService.addItem(tripId, category, itemName, assignedToMemberId, notes));
        } catch (ResponseStatusException e) {
            return error(e);
        }
    }

    // เพิ่มหลายชิ้นในครั้งเดียว (JSON): หมวด/ผู้รับผิดชอบใช้ร่วมกัน, จำนวน/หน่วย/โน้ตแยกต่อชิ้น
    @PostMapping("/bulk")
    public ResponseEntity<?> addItems(@RequestBody ChecklistBulkRequest request) {
        try {
            guard.me(request.getTripId());
            return ResponseEntity.ok(checklistService.addItems(
                    request.getTripId(),
                    request.getCategory(),
                    request.getItems(),
                    request.getAssigneeIds()));
        } catch (ResponseStatusException e) {
            return error(e);
        }
    }

    // แก้จำนวน/หน่วย/โน้ตของชิ้นนี้ (JSON)
    @PatchMapping("/{itemId}/details")
    public ResponseEntity<?> updateDetails(@PathVariable Long itemId, @RequestBody ChecklistDetailsRequest request) {
        try {
            Long me = guard.self(guard.tripOfChecklistItem(itemId), request.getMemberId()).getId();
            return ResponseEntity.ok(checklistService.updateDetails(itemId, request.getQuantity(),
                    request.getUnit(), request.getNotes(), me));
        } catch (ResponseStatusException e) {
            return error(e);
        }
    }

    // ตั้งผู้รับผิดชอบทั้งชุด (JSON array ของ memberId, [] = ยังไม่มีใครรับ)
    @PutMapping("/{itemId}/assignees")
    public ResponseEntity<?> setAssignees(@PathVariable Long itemId, @RequestBody List<Long> memberIds) {
        try {
            guard.me(guard.tripOfChecklistItem(itemId));
            return ResponseEntity.ok(checklistService.setAssignees(itemId, memberIds));
        } catch (ResponseStatusException e) {
            return error(e);
        }
    }

    @PatchMapping("/{itemId}/notes")
    public ResponseEntity<?> updateNotes(
            @PathVariable Long itemId,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) Long memberId) {
        try {
            Long me = guard.self(guard.tripOfChecklistItem(itemId), memberId).getId();
            return ResponseEntity.ok(checklistService.updateNotes(itemId, notes, me));
        } catch (ResponseStatusException e) {
            return error(e);
        }
    }

    @PatchMapping("/{itemId}/toggle")
    public ResponseEntity<?> toggleCheck(
            @PathVariable Long itemId,
            @RequestParam(required = false) Long memberId) {
        try {
            Long me = guard.self(guard.tripOfChecklistItem(itemId), memberId).getId();
            return ResponseEntity.ok(checklistService.toggleCheckStatus(itemId, me));
        } catch (ResponseStatusException e) {
            return error(e);
        }
    }

    // เปลี่ยนคนรับผิดชอบ ไม่ส่ง memberId = ยกเลิกคนรับผิดชอบ
    @PatchMapping("/{itemId}/assign")
    public ResponseEntity<?> assignItem(
            @PathVariable Long itemId,
            @RequestParam(required = false) Long memberId) {
        try {
            guard.me(guard.tripOfChecklistItem(itemId)); // มอบให้เพื่อนได้ แต่ต้องอยู่ทริปเดียวกัน
            return ResponseEntity.ok(checklistService.assignItem(itemId, memberId));
        } catch (ResponseStatusException e) {
            return error(e);
        }
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<?> deleteItem(@PathVariable Long itemId) {
        try {
            guard.me(guard.tripOfChecklistItem(itemId));
            checklistService.deleteItem(itemId);
            return ResponseEntity.ok("ลบรายการสิ่งของสำเร็จ");
        } catch (ResponseStatusException e) {
            return error(e);
        }
    }

    // ส่งข้อความภาษาไทยกลับไปให้หน้าเว็บแสดงได้ตรง ๆ
    private ResponseEntity<String> error(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
    }
}
