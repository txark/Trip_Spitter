package com.cp.party_trip.event;

import com.cp.party_trip.service.TripEventService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// Observer: ฟังเหตุการณ์ของทริปแล้วเขียนเป็นประวัติความเคลื่อนไหว
// ExpenseServiceImpl / RepaymentServiceImpl ไม่รู้จักตารางประวัติเลย แค่ประกาศเหตุการณ์ (ผู้ฟังเพิ่มได้โดยไม่แก้ผู้ส่ง)
// @EventListener ทำงานใน transaction เดียวกับผู้ส่ง: ถ้าบันทึกบิลล้มเหลว ประวัติก็ไม่ถูกเขียน
@Component
public class TripEventListener {
    private final TripEventService events;

    public TripEventListener(TripEventService events) {
        this.events = events;
    }

    @EventListener
    public void onExpenseAdded(ExpenseAddedEvent e) {
        events.record(e.tripId(), e.memberId(), "EXPENSE_ADDED",
                e.memberName() + " เพิ่มบิล “" + e.title() + "” " + e.amount() + " บาท");
    }

    @EventListener
    public void onRepaymentRecorded(RepaymentRecordedEvent e) {
        events.record(e.tripId(), e.receiverId(), "REPAYMENT_RECORDED",
                e.receiverName() + " รับเงินคืนจาก " + e.senderName() + " " + e.amount() + " บาท");
    }
}
