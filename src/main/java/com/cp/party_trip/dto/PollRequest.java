package com.cp.party_trip.dto;

import java.util.List;

// ข้อมูลสร้างโหวตใหม่: คำถาม + ตัวเลือกเริ่มต้น (อย่างน้อย 2 ข้อ)
public class PollRequest {
    private Long tripId;
    private Long memberId;
    private String question;
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
