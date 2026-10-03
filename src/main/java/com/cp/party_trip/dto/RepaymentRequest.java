package com.cp.party_trip.dto;

import java.math.BigDecimal;
import java.util.List;

// รับเงินเป็นยอดรวม: expenseIds = รายการที่จะหักให้ (ไม่ส่ง = ทุกรายการที่ค้าง) หักจากบิลเก่าสุดก่อน
public class RepaymentRequest {
    private BigDecimal amount;
    private List<Long> expenseIds;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public List<Long> getExpenseIds() {
        return expenseIds;
    }

    public void setExpenseIds(List<Long> expenseIds) {
        this.expenseIds = expenseIds;
    }
}
