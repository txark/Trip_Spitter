package com.cp.party_trip.repository;

import com.cp.party_trip.model.ChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChecklistItemRepo extends JpaRepository<ChecklistItem, Long> {
    List<ChecklistItem> findByTripId(Long tripId);

    // ค้นหาและกรองตามหมวดหมู่
    List<ChecklistItem> findByTripIdAndCategory(Long tripId, String category);

    // ค้นหาด้วยชื่อสิ่งของ (รองรับการพิมพ์บางส่วนและไม่สนตัวพิมพ์เล็กใหญ่)
    List<ChecklistItem> findByTripIdAndItemNameContainingIgnoreCase(Long tripId, String keyword);

    // ค้นหาด้วยโน้ต (รองรับการพิมพ์บางส่วนและไม่สนตัวพิมพ์เล็กใหญ่)
    List<ChecklistItem> findByTripIdAndNotesContainingIgnoreCase(Long tripId, String keyword);
}