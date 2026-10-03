package com.cp.party_trip.config;

import com.cp.party_trip.model.TripMember;
import com.cp.party_trip.model.User;
import com.cp.party_trip.repository.ActivityRepo;
import com.cp.party_trip.repository.ChecklistItemRepo;
import com.cp.party_trip.repository.ExpenseRepo;
import com.cp.party_trip.repository.PollRepo;
import com.cp.party_trip.repository.RepaymentRepo;
import com.cp.party_trip.repository.TripMemberRepo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

// ใช้ในคอนโทรลเลอร์: "ใครเป็นคนทำรายการนี้" มาจาก token (AuthInterceptor) ไม่ใช่ memberId ที่หน้าเว็บส่งมา
// - me(tripId)        = สมาชิกของผู้ใช้ในทริปนั้น (ไม่ใช่สมาชิก = 403)
// - self(tripId, id)  = เหมือน me แต่ถ้าหน้าเว็บส่ง memberId มาต้องเป็นตัวเอง (ทำแทนคนอื่นไม่ได้)
// - tripOfXxx(id)     = ทริปของข้อมูลชิ้นนั้น (ไม่พบ = 404)
@Component
public class AuthGuard {
    public static final String HEADER = "X-Auth-Token";
    static final String ATTRIBUTE = "authUser";

    private final HttpServletRequest request;
    private final TripMemberRepo tripMemberRepo;
    private final ActivityRepo activityRepo;
    private final ExpenseRepo expenseRepo;
    private final PollRepo pollRepo;
    private final ChecklistItemRepo checklistItemRepo;
    private final RepaymentRepo repaymentRepo;

    public AuthGuard(HttpServletRequest request, TripMemberRepo tripMemberRepo, ActivityRepo activityRepo,
            ExpenseRepo expenseRepo, PollRepo pollRepo, ChecklistItemRepo checklistItemRepo,
            RepaymentRepo repaymentRepo) {
        this.request = request; // proxy ของ request ปัจจุบัน
        this.tripMemberRepo = tripMemberRepo;
        this.activityRepo = activityRepo;
        this.expenseRepo = expenseRepo;
        this.pollRepo = pollRepo;
        this.checklistItemRepo = checklistItemRepo;
        this.repaymentRepo = repaymentRepo;
    }

    public User user() {
        Object user = request.getAttribute(ATTRIBUTE);
        if (user instanceof User u) {
            return u;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "กรุณาเข้าสู่ระบบใหม่ที่หน้าแรก");
    }

    public TripMember me(Long tripId) {
        if (tripId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ไม่ได้ระบุทริป");
        }
        return tripMemberRepo.findFirstByTripIdAndGuestNameOrderByIdAsc(tripId, user().getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "คุณไม่ได้เป็นสมาชิกในทริปนี้"));
    }

    public TripMember self(Long tripId, Long memberId) {
        TripMember me = me(tripId);
        if (memberId != null && !memberId.equals(me.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ทำรายการแทนสมาชิกคนอื่นไม่ได้");
        }
        return me;
    }

    public Long tripOfActivity(Long activityId) {
        return activityRepo.findById(activityId)
                .map(a -> a.getTrip() == null ? null : a.getTrip().getId())
                .orElseThrow(() -> notFound("ไม่พบกิจกรรมนี้"));
    }

    public Long tripOfExpense(Long expenseId) {
        return expenseRepo.findTripIdById(expenseId)
                .orElseThrow(() -> notFound("ไม่พบบิลนี้"));
    }

    public Long tripOfPoll(Long pollId) {
        return pollRepo.findById(pollId).map(p -> p.getTripId())
                .orElseThrow(() -> notFound("ไม่พบโหวตนี้"));
    }

    public Long tripOfChecklistItem(Long itemId) {
        return checklistItemRepo.findById(itemId).map(i -> i.getTripId())
                .orElseThrow(() -> notFound("ไม่พบรายการนี้"));
    }

    public Long tripOfRepayment(Long repaymentId) {
        return repaymentRepo.findById(repaymentId).map(r -> r.getTripId())
                .orElseThrow(() -> notFound("ไม่พบรายการรับเงินนี้"));
    }

    private static ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
