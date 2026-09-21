package com.cp.party_trip.service;

import com.cp.party_trip.dto.PollResultDTO;
import com.cp.party_trip.model.*;
import com.cp.party_trip.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class PollService {
    private final PollRepo pollRepo;
    private final PollOptionRepo pollOptionRepo;
    private final PollVoteRepo pollVoteRepo;

    public PollService(PollRepo pollRepo, PollOptionRepo pollOptionRepo, PollVoteRepo pollVoteRepo) {
        this.pollRepo = pollRepo;
        this.pollOptionRepo = pollOptionRepo;
        this.pollVoteRepo = pollVoteRepo;
    }

    @Transactional
    public PollVote castVote(Long pollId, Long optionId, Long memberId) {
        Poll poll = pollRepo.findById(pollId)
                .orElseThrow(() -> new RuntimeException("ไม่พบหัวข้อโหวตนี้"));

        if ("CLOSED".equals(poll.getStatus())) {
            throw new RuntimeException("โหวตนี้ถูกปิดรับคะแนนแล้ว");
        }

        if (pollVoteRepo.existsByPollIdAndMemberId(pollId, memberId)) {
            throw new RuntimeException("คุณลงคะแนนในหัวข้อนี้ไปแล้ว");
        }

        PollVote vote = new PollVote();
        vote.setPollId(pollId);
        vote.setOptionId(optionId);
        vote.setMemberId(memberId);
        return pollVoteRepo.save(vote);
    }

    public List<PollResultDTO> getPollResults(Long pollId) {
        List<PollOption> options = pollOptionRepo.findByPollId(pollId);
        List<PollResultDTO> results = new ArrayList<>();

        for (PollOption option : options) {
            int count = pollVoteRepo.countByOptionId(option.getId());
            results.add(new PollResultDTO(option.getId(), option.getOptionText(), count));
        }
        return results;
    }

    @Transactional
    public PollOption addOptionToPoll(Long pollId, String newOptionText) {
        Poll poll = pollRepo.findById(pollId)
                .orElseThrow(() -> new RuntimeException("ไม่พบหัวข้อโหวตนี้"));

        if ("CLOSED".equals(poll.getStatus())) {
            throw new RuntimeException("ไม่สามารถเสนอตัวเลือกเพิ่มได้ เพราะโหวตถูกปิดแล้ว");
        }

        PollOption option = new PollOption();
        option.setPollId(pollId);
        option.setOptionText(newOptionText);
        return pollOptionRepo.save(option);
    }

    @Transactional
    public Poll createPoll(Long tripId, String question) {
        Poll poll = new Poll();
        poll.setTripId(tripId);
        poll.setQuestion(question);
        poll.setStatus("ACTIVE"); // เปิดโหวตทันทีที่สร้าง
        return pollRepo.save(poll);
    }
}