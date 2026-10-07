package com.cp.party_trip.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.cp.party_trip.config.AuthGuard;
import com.cp.party_trip.dto.request.PollRequest;
import com.cp.party_trip.dto.response.MessageResponse;
import com.cp.party_trip.dto.response.PollOptionResponse;
import com.cp.party_trip.dto.response.PollResponse;
import com.cp.party_trip.dto.response.PollResultResponse;
import com.cp.party_trip.dto.response.PollSummaryResponse;
import com.cp.party_trip.mapper.PollMapper;
import com.cp.party_trip.model.PollVote;
import com.cp.party_trip.service.PollService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Tag(name = "Polls - โหวต")
@RestController
@RequestMapping("/api/v1/polls")
public class PollController {
    private final PollService pollService;
    private final PollMapper pollMapper;
    private final AuthGuard guard;

    public PollController(PollService pollService, PollMapper pollMapper, AuthGuard guard) {
        this.pollService = pollService;
        this.pollMapper = pollMapper;
        this.guard = guard;
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
            PollVote vote = pollService.castVote(pollId, optionId, memberId);
            return ResponseEntity.ok(new MessageResponse(vote == null ? "ยกเลิกโหวตแล้ว" : "บันทึกคะแนนโหวตสำเร็จ"));
        } catch (DataIntegrityViolationException e) {
            // กดโหวตพร้อมกัน 2 ครั้ง (เช่น 2 แท็บ) ชน unique (poll_id, member_id)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "มีการโหวตซ้อนกัน กรุณาลองใหม่อีกครั้ง");
        }
    }

    @GetMapping("/{pollId}/results")
    public ResponseEntity<List<PollResultResponse>> getResults(@PathVariable Long pollId) {
        guard.me(guard.tripOfPoll(pollId));
        return ResponseEntity.ok(pollService.getPollResults(pollId));
    }

    @PostMapping("/{pollId}/options")
    public ResponseEntity<PollOptionResponse> addOption(
            @PathVariable Long pollId,
            @RequestParam String optionText,
            @RequestParam(required = false) Long memberId) {
        Long me = guard.self(guard.tripOfPoll(pollId), memberId).getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pollMapper.toResponse(pollService.addOptionToPoll(pollId, optionText, me)));
    }

    @PostMapping
    public ResponseEntity<PollResponse> createPoll(@Valid @RequestBody PollRequest request) {
        request.setMemberId(guard.self(request.getTripId(), request.getMemberId()).getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(pollMapper.toResponse(pollService.createPoll(request)));
    }

    @PutMapping("/{pollId}/close")
    public ResponseEntity<PollResponse> closePoll(@PathVariable Long pollId, @RequestParam Long memberId) {
        guard.self(guard.tripOfPoll(pollId), memberId);
        return ResponseEntity.ok(pollMapper.toResponse(pollService.closePoll(pollId, memberId)));
    }

    @DeleteMapping("/{pollId}")
    public ResponseEntity<Void> deletePoll(@PathVariable Long pollId, @RequestParam Long memberId) {
        guard.self(guard.tripOfPoll(pollId), memberId);
        pollService.deletePoll(pollId, memberId);
        return ResponseEntity.noContent().build();
    }
}
