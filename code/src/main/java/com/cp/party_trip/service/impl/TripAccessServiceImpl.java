package com.cp.party_trip.service.impl;

import com.cp.party_trip.model.TripMember;
import com.cp.party_trip.repository.ActivityRepo;
import com.cp.party_trip.repository.ChecklistItemRepo;
import com.cp.party_trip.repository.ExpenseRepo;
import com.cp.party_trip.repository.PollRepo;
import com.cp.party_trip.repository.RepaymentRepo;
import com.cp.party_trip.repository.TripMemberRepo;
import com.cp.party_trip.service.TripAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class TripAccessServiceImpl implements TripAccessService {
    private final TripMemberRepo tripMemberRepo;
    private final ActivityRepo activityRepo;
    private final ExpenseRepo expenseRepo;
    private final PollRepo pollRepo;
    private final ChecklistItemRepo checklistItemRepo;
    private final RepaymentRepo repaymentRepo;

    public TripAccessServiceImpl(TripMemberRepo tripMemberRepo, ActivityRepo activityRepo, ExpenseRepo expenseRepo,
            PollRepo pollRepo, ChecklistItemRepo checklistItemRepo, RepaymentRepo repaymentRepo) {
        this.tripMemberRepo = tripMemberRepo;
        this.activityRepo = activityRepo;
        this.expenseRepo = expenseRepo;
        this.pollRepo = pollRepo;
        this.checklistItemRepo = checklistItemRepo;
        this.repaymentRepo = repaymentRepo;
    }

    @Override
    public TripMember requireMember(Long tripId, String username) {
        return tripMemberRepo.findFirstByTripIdAndGuestNameOrderByIdAsc(tripId, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "คุณไม่ได้เป็นสมาชิกในทริปนี้"));
    }

    @Override
    public Long tripOfActivity(Long activityId) {
        return activityRepo.findById(activityId)
                .map(a -> a.getTrip() == null ? null : a.getTrip().getId())
                .orElseThrow(() -> notFound("ไม่พบกิจกรรมนี้"));
    }

    @Override
    public Long tripOfExpense(Long expenseId) {
        return expenseRepo.findTripIdById(expenseId)
                .orElseThrow(() -> notFound("ไม่พบบิลนี้"));
    }

    @Override
    public Long tripOfPoll(Long pollId) {
        return pollRepo.findById(pollId).map(p -> p.getTripId())
                .orElseThrow(() -> notFound("ไม่พบโหวตนี้"));
    }

    @Override
    public Long tripOfChecklistItem(Long itemId) {
        return checklistItemRepo.findById(itemId).map(i -> i.getTripId())
                .orElseThrow(() -> notFound("ไม่พบรายการนี้"));
    }

    @Override
    public Long tripOfRepayment(Long repaymentId) {
        return repaymentRepo.findById(repaymentId).map(r -> r.getTripId())
                .orElseThrow(() -> notFound("ไม่พบรายการรับเงินนี้"));
    }

    private static ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
