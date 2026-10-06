package com.cp.party_trip.service;

import com.cp.party_trip.dto.request.PollRequest;
import com.cp.party_trip.dto.response.PollResultResponse;
import com.cp.party_trip.dto.response.PollSummaryResponse;
import com.cp.party_trip.model.Poll;
import com.cp.party_trip.model.PollOption;
import com.cp.party_trip.model.PollVote;

import java.util.List;

// โหวตในทริป: สร้างโหวต ลงคะแนน ปิดโหวต
// ตัวจริงอยู่ที่ service/impl/PollServiceImpl (Controller ขึ้นกับ interface นี้ ไม่ใช่คลาสจริง)
public interface PollService {
    // โหวต: ยังไม่เคยโหวต = บันทึกใหม่, เลือกข้อเดิมซ้ำ = ยกเลิกโหวต, เลือกข้ออื่น = เปลี่ยนโหวต
    // คืนค่า null เมื่อเป็นการยกเลิกโหวต
    PollVote castVote(Long pollId, Long optionId, Long memberId);

    List<PollResultResponse> getPollResults(Long pollId);

    // โหวตทั้งหมดในทริป (ใหม่สุดก่อน) พร้อมผลคะแนนและข้อที่สมาชิกคนนี้เลือก
    List<PollSummaryResponse> getTripPolls(Long tripId, Long memberId);

    PollOption addOptionToPoll(Long pollId, String newOptionText, Long memberId);

    Poll createPoll(PollRequest request);

    // ปิดโหวต: เฉพาะคนสร้าง (โหวตเก่าที่ไม่มีคนสร้าง สมาชิกในทริปปิดได้)
    Poll closePoll(Long pollId, Long memberId);

    void deletePoll(Long pollId, Long memberId);
}
