package com.cp.party_trip.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

// ข้อมูลสร้างโหวตใหม่: คำถาม + ตัวเลือกเริ่มต้น (อย่างน้อย 2 ข้อ)
public class PollRequest {
    @NotNull(message = "ไม่ได้ระบุทริป")
    private Long tripId;
    private Long memberId;
    @NotBlank(message = "กรุณาใส่คำถาม")
    @Size(max = 150, message = "คำถามยาวได้ไม่เกิน 150 ตัวอักษร")
    private String question;
    @Size(max = 10, message = "ตัวเลือกได้ไม่เกิน 10 ข้อ")
    private List<String> options;
    private Integer durationMinutes; // ระยะเวลาเปิดโหวต (null = ไม่จำกัดเวลา)

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }
}
