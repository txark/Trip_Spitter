package com.cp.party_trip.service.split;

import com.cp.party_trip.dto.request.ExpenseRequest;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// แต่ละคนจ่ายไม่เท่ากัน ตามยอดที่กรอกมา (ผลรวมต้องเท่ากับยอดบิล)
@Component
@Order(2)
public class CustomSplitStrategy implements SplitStrategy {
    @Override
    public String type() {
        return "CUSTOM";
    }

    @Override
    public List<SplitShare> split(SplitContext context) {
        List<ExpenseRequest.SplitAmount> custom = context.customAmounts();
        if (custom == null || custom.isEmpty()) {
            throw badRequest("กรุณาระบุยอดของผู้ร่วมหารแต่ละคน");
        }

        BigDecimal sum = BigDecimal.ZERO;
        Set<Long> seen = new HashSet<>();
        for (ExpenseRequest.SplitAmount s : custom) {
            if (s.getMemberId() == null || !seen.add(s.getMemberId())) {
                throw badRequest("รายชื่อผู้ร่วมหารไม่ถูกต้อง");
            }
            if (s.getAmount() == null || s.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw badRequest("ยอดของแต่ละคนต้องไม่ติดลบ");
            }
            if (s.getAmount().stripTrailingZeros().scale() > 2) {
                throw badRequest("ยอดของแต่ละคนใส่ทศนิยมได้ไม่เกิน 2 ตำแหน่ง");
            }
            sum = sum.add(s.getAmount());
        }
        if (sum.setScale(2, RoundingMode.HALF_UP).compareTo(context.totalAmount()) != 0) {
            throw badRequest("ผลรวมของแต่ละคน (" + sum + ") ไม่เท่ากับยอดบิล (" + context.totalAmount() + ")");
        }

        List<SplitShare> shares = new ArrayList<>();
        for (ExpenseRequest.SplitAmount s : custom) {
            // คนที่ยอด 0 ไม่ต้องสร้าง split (ไม่ได้ร่วมจ่ายบิลนี้)
            if (s.getAmount().compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            shares.add(new SplitShare(s.getMemberId(), s.getAmount().setScale(2, RoundingMode.HALF_UP)));
        }
        return shares;
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
