package com.cp.party_trip.config;

import com.cp.party_trip.model.TripMember;
import com.cp.party_trip.model.User;
import com.cp.party_trip.service.TripAccessService;
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
    private final TripAccessService access;

    public AuthGuard(HttpServletRequest request, TripAccessService access) {
        this.request = request; // proxy ของ request ปัจจุบัน
        this.access = access;
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
        return access.requireMember(tripId, user().getUsername());
    }

    public TripMember self(Long tripId, Long memberId) {
        TripMember me = me(tripId);
        if (memberId != null && !memberId.equals(me.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ทำรายการแทนสมาชิกคนอื่นไม่ได้");
        }
        return me;
    }

    public Long tripOfActivity(Long activityId) {
        return access.tripOfActivity(activityId);
    }

    public Long tripOfExpense(Long expenseId) {
        return access.tripOfExpense(expenseId);
    }

    public Long tripOfPoll(Long pollId) {
        return access.tripOfPoll(pollId);
    }

    public Long tripOfChecklistItem(Long itemId) {
        return access.tripOfChecklistItem(itemId);
    }

    public Long tripOfRepayment(Long repaymentId) {
        return access.tripOfRepayment(repaymentId);
    }
}
