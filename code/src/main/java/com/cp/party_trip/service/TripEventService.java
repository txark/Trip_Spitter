package com.cp.party_trip.service;

import com.cp.party_trip.model.TripEvent;

import java.util.List;

public interface TripEventService {
    void record(Long tripId, Long memberId, String type, String message);

    // 20 รายการล่าสุดของทริป ใหม่สุดก่อน
    List<TripEvent> recent(Long tripId);
}
