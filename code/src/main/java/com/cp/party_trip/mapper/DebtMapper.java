package com.cp.party_trip.mapper;

import com.cp.party_trip.dto.response.DebtTransferResponse;
import com.cp.party_trip.dto.response.RepaymentResponse;
import com.cp.party_trip.model.DebtTransfer;
import com.cp.party_trip.model.Repayment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DebtMapper {
    private final MemberMapper memberMapper;

    public DebtMapper(MemberMapper memberMapper) {
        this.memberMapper = memberMapper;
    }

    public List<DebtTransferResponse> toResponses(List<DebtTransfer> transfers) {
        return transfers.stream()
                .map(t -> new DebtTransferResponse(memberMapper.toResponse(t.getFrom()),
                        memberMapper.toResponse(t.getTo()), t.getAmount()))
                .toList();
    }

    public RepaymentResponse toResponse(Repayment r) {
        List<RepaymentResponse.Item> items = r.getItems().stream()
                .map(i -> new RepaymentResponse.Item(i.getExpenseId(), i.getSplitId(), i.getTitle(), i.getAmount()))
                .toList();
        return new RepaymentResponse(r.getId(), r.getTripId(), r.getFromMemberId(), r.getToMemberId(), r.getAmount(),
                r.getCreatedAt(), items);
    }
}
