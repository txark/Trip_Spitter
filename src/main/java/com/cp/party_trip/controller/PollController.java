package com.cp.party_trip.controller;

import com.cp.party_trip.dto.PollResultDTO;
import com.cp.party_trip.model.PollOption;
import com.cp.party_trip.service.PollService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/polls")
public class PollController {
    private final PollService pollService;

    public PollController(PollService pollService) {
        this.pollService = pollService;
    }

    @PostMapping("/{pollId}/vote")
    public ResponseEntity<?> vote(
            @PathVariable Long pollId,
            @RequestParam Long optionId,
            @RequestParam Long memberId) {
        try {
            pollService.castVote(pollId, optionId, memberId);
            return ResponseEntity.ok("บันทึกคะแนนโหวตสำเร็จ");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{pollId}/results")
    public ResponseEntity<List<PollResultDTO>> getResults(@PathVariable Long pollId) {
        return ResponseEntity.ok(pollService.getPollResults(pollId));
    }

    @PostMapping("/{pollId}/options")
    public ResponseEntity<?> addOption(
            @PathVariable Long pollId,
            @RequestParam String optionText) {
        try {
            PollOption newOption = pollService.addOptionToPoll(pollId, optionText);
            return ResponseEntity.ok(newOption);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<com.cp.party_trip.model.Poll> createPoll(
            @RequestParam Long tripId,
            @RequestParam String question) {
        try {
            com.cp.party_trip.model.Poll newPoll = pollService.createPoll(tripId, question);
            return ResponseEntity.ok(newPoll);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}