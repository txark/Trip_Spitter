package com.cp.party_trip.dto.response;

import java.math.BigDecimal;

// ใครต้องโอนให้ใครเท่าไหร่
public record DebtTransferResponse(MemberResponse from, MemberResponse to, BigDecimal amount) {
}
