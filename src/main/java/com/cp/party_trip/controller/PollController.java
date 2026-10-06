package com.cp.party_trip.controller;

import com.cp.party_trip.dto.PollRequest;
import com.cp.party_trip.dto.PollResultDTO;
import com.cp.party_trip.dto.PollSummaryDTO;
import com.cp.party_trip.model.Poll;
import com.cp.party_trip.model.PollOption;
import com.cp.party_trip.model.PollVote;
import com.cp.party_trip.service.PollService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/polls")
public class PollController {
    private final PollService pollService;

    public PollController(PollService pollService) {
        this.pollService = pollService;
    }

    // โหวตทั้งหมดของทริป + ข้อที่ memberId เลือก
    @GetMapping("/trip/{tripId}")
    public ResponseEntity<List<PollSummaryResponse>> getTripPolls(
            @PathVariable Long tripId,
            @RequestParam(required = false) Long memberId) {
        guard.self(tripId, memberId);
        return ResponseEntity.ok(pollService.getTripPolls(tripId, memberId));
    }

    @PostMapping("/{pollId}/vote")
    public ResponseEntity<MessageResponse> vote(
            @PathVariable Long pollId,
            @RequestParam Long optionId,
            @RequestParam Long memberId) {
        guard.self(guard.tripOfPoll(pollId), memberId);
        try {
            guard.self(guard.tripOfPoll(pollId), memberId);
            PollVote vote = pollService.castVote(pollId, optionId, memberId);
            return ResponseEntity.ok(new MessageResponse(vote == null ? "ยกเลิกโหวตแล้ว" : "บันทึกคะแนนโหวตสำเร็จ"));
        } catch (DataIntegrityViolationException e) {
            // กดโหวตพร้อมกัน 2 ครั้ง (เช่น 2 แท็บ) ชน unique (poll_id, member_id)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "มีการโหวตซ้อนกัน กรุณาลองใหม่อีกครั้ง");
        }
    }

    @GetMapping("/{pollId}/results")
    public ResponseEntity<?> getResults(@PathVariable Long pollId) {
        try {
            List<PollResultDTO> results = pollService.getPollResults(pollId);
            return ResponseEntity.ok(results);
        } catch (ResponseStatusException e) {
            return error(e);
        }
    }

    @PostMapping("/{pollId}/options")
    public ResponseEntity<PollOptionResponse> addOption(
            @PathVariable Long pollId,
            @RequestParam String optionText,
            @RequestParam(required = false) Long memberId) {
        try {
            PollOption newOption = pollService.addOptionToPoll(pollId, optionText, memberId);
            return ResponseEntity.ok(newOption);
        } catch (ResponseStatusException e) {
            return error(e);
        }
    }

    @PostMapping
    public ResponseEntity<?> createPoll(@RequestBody PollRequest request) {
        try {
            Poll newPoll = pollService.createPoll(request);
            return ResponseEntity.ok(newPoll);
        } catch (ResponseStatusException e) {
            return error(e);
        }
    }

    @PutMapping("/{pollId}/close")
    public ResponseEntity<?> closePoll(@PathVariable Long pollId, @RequestParam Long memberId) {
        try {
            return ResponseEntity.ok(pollService.closePoll(pollId, memberId));
        } catch (ResponseStatusException e) {
            return error(e);
        }
    }

    @DeleteMapping("/{pollId}")
    public ResponseEntity<?> deletePoll(@PathVariable Long pollId, @RequestParam Long memberId) {
        try {
            pollService.deletePoll(pollId, memberId);
            return ResponseEntity.ok("ลบโหวตแล้ว");
        } catch (ResponseStatusException e) {
            return error(e);
        }
    }

    // ส่งข้อความภาษาไทยกลับไปให้หน้าเว็บแสดงได้ตรง ๆ
    private ResponseEntity<String> error(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
    }
}
