package com.cp.party_trip.service.split;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Factory: เลือก SplitStrategy จากรหัส splitType ที่หน้าเว็บส่งมา
// Spring ส่ง strategy ทุกตัวที่เป็น @Component เข้ามาเอง จึงไม่มี if-else/switch ตามชนิดการหาร
@Component
public class SplitStrategyFactory {
    private final Map<String, SplitStrategy> strategies = new LinkedHashMap<>();

    public SplitStrategyFactory(List<SplitStrategy> all) {
        all.forEach(s -> strategies.put(s.type(), s));
    }

    // ไม่ระบุวิธีหาร = หารเท่ากัน, ระบุแบบที่ไม่รองรับ = 400
    public SplitStrategy forType(String splitType) {
        String type = splitType == null || splitType.isBlank() ? "EQUAL" : splitType.trim().toUpperCase();
        SplitStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "วิธีหารไม่ถูกต้อง (รองรับ " + String.join(" หรือ ", strategies.keySet()) + ")");
        }
        return strategy;
    }
}
