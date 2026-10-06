package com.cp.party_trip.mapper;

import com.cp.party_trip.dto.response.ExpenseResponse;
import com.cp.party_trip.dto.response.ExpenseViewResponse;
import com.cp.party_trip.model.Expense;
import com.cp.party_trip.model.ExpenseSplit;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ExpenseMapper {
    private final MemberMapper memberMapper;

    public ExpenseMapper(MemberMapper memberMapper) {
        this.memberMapper = memberMapper;
    }

    // บิลที่เพิ่งบันทึก/แก้ (ข้อมูลสมาชิกเต็ม)
    public ExpenseResponse toResponse(Expense e) {
        List<ExpenseResponse.Split> splits = e.getExpenseSplits() == null ? List.of()
                : e.getExpenseSplits().stream()
                        .map(s -> new ExpenseResponse.Split(s.getId(), s.getTripMemberId(),
                                memberMapper.toResponse(s.getTripMember()), s.getAmountOwed(), s.getPercentage(),
                                s.isPaid(), s.getPaidAmount()))
                        .toList();
        return new ExpenseResponse(e.getId(), e.getTitle(), e.getTotalAmount(), e.getCurrency(),
                e.getOriginalAmount(), e.getExchangeRate(), e.getSplitType(), e.getCategory(), e.getExpenseDate(),
                e.getActivityId(), e.getRecordedById(), e.getRevision(), memberMapper.toResponse(e.getUser()),
                splits);
    }

    // บิลในรายการบิลของทริป (splits ส่งมาแยก ดึงจาก repo ตรง ไม่พึ่ง lazy collection)
    public ExpenseViewResponse toView(Expense e, List<ExpenseSplit> splits) {
        List<ExpenseViewResponse.Split> views = splits == null ? List.of()
                : splits.stream()
                        .map(s -> new ExpenseViewResponse.Split(s.getId(), s.getAmountOwed(), s.isPaid(),
                                s.paidSoFar(), memberMapper.toRef(s.getTripMember())))
                        .toList();
        return new ExpenseViewResponse(e.getId(), e.getTitle(), e.getTotalAmount(), e.getCategory(),
                e.getSplitType(), e.getExpenseDate(), e.getActivityId(), e.getRecordedById(), e.getRevision(),
                e.getCurrency(), e.getOriginalAmount(), e.getExchangeRate(), memberMapper.toRef(e.getUser()), views,
                views);
    }
}
