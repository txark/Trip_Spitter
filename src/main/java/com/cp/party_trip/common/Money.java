package com.cp.party_trip.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

// สกุลเงินและอัตราแลกเปลี่ยน: เงินหลักของระบบคือบาท (หนี้/งบคิดเป็นบาทเสมอ)
// ยอดเงินต่างประเทศเก็บคู่กับเรท "1 หน่วย = กี่บาท" ไว้แสดงยอดตามใบเสร็จ
public final class Money {
    public static final String BASE = "THB";
    static final BigDecimal MAX_RATE = new BigDecimal("100000");
    private static final Pattern CODE = Pattern.compile("[A-Z]{3}");

    private Money() {
    }

    // null = บาท, ที่เหลือต้องเป็นรหัส 3 ตัวอักษร (เช่น JPY)
    public static String currency(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String c = code.trim().toUpperCase();
        if (BASE.equals(c)) {
            return null;
        }
        if (!CODE.matcher(c).matches()) {
            throw badRequest("สกุลเงินไม่ถูกต้อง");
        }
        return c;
    }

    // เรทต้องมากกว่า 0 เก็บทศนิยม 6 ตำแหน่ง (เงินดอง/รูเปียห์ 1 หน่วยไม่ถึง 0.01 บาท)
    public static BigDecimal rate(BigDecimal rate) {
        if (rate == null || rate.signum() <= 0 || rate.compareTo(MAX_RATE) > 0) {
            throw badRequest("อัตราแลกเปลี่ยนต้องมากกว่า 0");
        }
        BigDecimal scaled = rate.setScale(6, RoundingMode.HALF_UP);
        if (scaled.signum() <= 0) {
            throw badRequest("อัตราแลกเปลี่ยนต้องมากกว่า 0");
        }
        return scaled;
    }

    public static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
