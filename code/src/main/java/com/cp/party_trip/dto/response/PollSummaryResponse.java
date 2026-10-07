package com.cp.party_trip.dto.response;

import java.time.LocalDateTime;
import java.util.List;

// โหวตหนึ่งหัวข้อพร้อมผลคะแนน สำหรับหน้า polls.html
public class PollSummaryResponse {
    private Long id;
    private String question;
    private String status;
    private Long createdByMemberId;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime closesAt;
    private Long remainingSeconds; // วินาทีที่เหลือก่อนปิด คำนวณที่เซิร์ฟเวอร์ กันนาฬิกาเครื่องผู้ใช้เพี้ยน (null = ไม่จำกัดเวลา)
    private int totalVotes;
    private Long myOptionId; // ข้อที่สมาชิกคนนี้เลือก (null = ยังไม่โหวต)
    private List<PollResultResponse> options;

    public PollSummaryResponse(Long id, String question, String status, Long createdByMemberId, String createdByName,
            LocalDateTime createdAt, LocalDateTime closesAt, Long remainingSeconds, int totalVotes, Long myOptionId,
            List<PollResultResponse> options) {
        this.id = id;
        this.question = question;
        this.status = status;
        this.createdByMemberId = createdByMemberId;
        this.createdByName = createdByName;
        this.createdAt = createdAt;
        this.closesAt = closesAt;
        this.remainingSeconds = remainingSeconds;
        this.totalVotes = totalVotes;
        this.myOptionId = myOptionId;
        this.options = options;
    }

    public Long getId() {
        return id;
    }

    public String getQuestion() {
        return question;
    }

    public String getStatus() {
        return status;
    }

    public Long getCreatedByMemberId() {
        return createdByMemberId;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getClosesAt() {
        return closesAt;
    }

    public Long getRemainingSeconds() {
        return remainingSeconds;
    }

    public int getTotalVotes() {
        return totalVotes;
    }

    public Long getMyOptionId() {
        return myOptionId;
    }

    public List<PollResultResponse> getOptions() {
        return options;
    }
}
