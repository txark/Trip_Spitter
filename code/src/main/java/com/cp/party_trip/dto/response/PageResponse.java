package com.cp.party_trip.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

// หน้าของข้อมูล (หน้าเริ่มที่ 0) รูปแบบคงที่ ไม่ผูกกับโครงสร้างภายในของ Spring Data
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
