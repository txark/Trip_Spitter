package com.cp.party_trip.service;

import com.cp.party_trip.dto.request.ChecklistBulkRequest;
import com.cp.party_trip.model.ChecklistItem;
import java.math.BigDecimal;
import java.util.List;

// เช็กลิสต์ของที่ต้องเตรียม: เพิ่ม ติ๊ก แบ่งคนรับผิดชอบ
// ตัวจริงอยู่ที่ service/impl/ChecklistServiceImpl (Controller ขึ้นกับ interface นี้ ไม่ใช่คลาสจริง)
public interface ChecklistService {
    List<ChecklistItem> getChecklistByTrip(Long tripId, String category, String search);

    // เพิ่มทีละชิ้น (endpoint เดิม) — ผู้รับผิดชอบ 1 คนหรือไม่มี
    ChecklistItem addItem(Long tripId, String category, String itemName, Long assignedToMemberId, String notes);

    // เพิ่มหลายชิ้นพร้อมกัน (ทั้งหมดหรือไม่เลย)
    // - ทุกชิ้นใช้หมวด/ผู้รับผิดชอบชุดเดียวกัน แต่จำนวน/หน่วย/โน้ตแยกต่อชิ้น
    // - ตัดแถวที่ไม่มีชื่อ และชื่อซ้ำในชุดเดียวกัน (ไม่สนตัวพิมพ์) ออก คงลำดับเดิม
    List<ChecklistItem> addItems(Long tripId, String category, List<ChecklistBulkRequest.NewItem> newItems,
            List<Long> assigneeIds);

    // แก้จำนวน/หน่วย/โน้ตของชิ้นนี้
    ChecklistItem updateDetails(Long itemId, BigDecimal quantity, String unit, String notes, Long memberId);

    ChecklistItem updateNotes(Long itemId, String notes, Long memberId);

    // ติ๊ก/ยกเลิกติ๊ก — memberId (ถ้ามี) บันทึกว่าใครเป็นคนกดล่าสุด
    ChecklistItem toggleCheckStatus(Long itemId, Long memberId);

    // ตั้งผู้รับผิดชอบทั้งชุด (รายการว่าง = ยังไม่มีใครรับ)
    ChecklistItem setAssignees(Long itemId, List<Long> memberIds);

    // endpoint เดิม: ผู้รับผิดชอบคนเดียว (memberId = null คือยังไม่มีใครรับ)
    ChecklistItem assignItem(Long itemId, Long memberId);

    void deleteItem(Long itemId);
}
