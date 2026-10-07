package com.cp.party_trip.service.split;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

// หารเท่ากันตามรายชื่อผู้ร่วมหาร
@Component
@Order(1)
public class EqualSplitStrategy implements SplitStrategy {
    @Override
    public String type() {
        return "EQUAL";
    }

    @Override
    public List<SplitShare> split(SplitContext context) {
        // ตัดรายชื่อซ้ำออก (ซ้ำแล้วคนเดียวจะมี 2 split และกดจ่ายได้แค่อันเดียว)
        // ไม่มีผู้ร่วมหารเลย = ผู้จ่ายออกเองทั้งหมด
        List<Long> people = new ArrayList<>(new LinkedHashSet<>(
                context.participantIds() == null ? List.<Long>of() : context.participantIds()));
        people.removeIf(Objects::isNull);
        if (people.isEmpty()) {
            people.add(context.payerId());
        }
        int count = people.size();
        BigDecimal perPerson = context.totalAmount().divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        // เศษจากการปัดทศนิยม (เช่น 100/3) ให้คนแรกรับไป เพื่อให้ผลรวมเท่ากับยอดบิลพอดี
        BigDecimal remainder = context.totalAmount().subtract(perPerson.multiply(BigDecimal.valueOf(count)));

        List<SplitShare> shares = new ArrayList<>();
        for (Long memberId : people) {
            shares.add(new SplitShare(memberId, shares.isEmpty() ? perPerson.add(remainder) : perPerson));
        }
        return shares;
    }
}
