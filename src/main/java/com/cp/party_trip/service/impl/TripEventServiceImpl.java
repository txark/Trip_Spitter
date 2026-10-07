package com.cp.party_trip.service.impl;

import com.cp.party_trip.model.TripEvent;
import com.cp.party_trip.repository.TripEventRepo;
import com.cp.party_trip.service.TripEventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TripEventServiceImpl implements TripEventService {
    private static final int MAX_MESSAGE = 255;

    private final TripEventRepo tripEventRepo;

    public TripEventServiceImpl(TripEventRepo tripEventRepo) {
        this.tripEventRepo = tripEventRepo;
    }

    @Override
    @Transactional
    public void record(Long tripId, Long memberId, String type, String message) {
        TripEvent event = new TripEvent();
        event.setTripId(tripId);
        event.setMemberId(memberId);
        event.setEventType(type);
        event.setMessage(message.length() > MAX_MESSAGE ? message.substring(0, MAX_MESSAGE) : message);
        tripEventRepo.save(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripEvent> recent(Long tripId) {
        return tripEventRepo.findTop20ByTripIdOrderByCreatedAtDescIdDesc(tripId);
    }
}
