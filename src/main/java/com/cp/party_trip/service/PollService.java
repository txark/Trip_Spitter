package com.cp.party_trip.service;

import com.cp.party_trip.dto.PollRequest;
import com.cp.party_trip.dto.PollResultDTO;
import com.cp.party_trip.dto.PollSummaryDTO;
import com.cp.party_trip.model.*;
import com.cp.party_trip.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PollService {
    static final int MAX_QUESTION_LENGTH = 150;
    static final int MAX_OPTION_LENGTH = 100;
    static final int MIN_OPTIONS = 2;
    static final int MAX_OPTIONS = 10;
    static final int MAX_DURATION_MINUTES = 30 * 24 * 60; // 30 วัน

    private final PollRepo pollRepo;
    private final PollOptionRepo pollOptionRepo;
    private final PollVoteRepo pollVoteRepo;
    private final TripMemberRepo tripMemberRepo;
    private Clock clock = Clock.systemDefaultZone();

    public PollService(PollRepo pollRepo, PollOptionRepo pollOptionRepo, PollVoteRepo pollVoteRepo,
            TripMemberRepo tripMemberRepo) {
        this.pollRepo = pollRepo;
        this.pollOptionRepo = pollOptionRepo;
        this.pollVoteRepo = pollVoteRepo;
        this.tripMemberRepo = tripMemberRepo;
    }

    // สำหรับเทสต์: กำหนดเวลาปัจจุบันเอง
    void setClock(Clock clock) {
        this.clock = clock;
    }

    // โหวต: ยังไม่เคยโหวต = บันทึกใหม่, เลือกข้อเดิมซ้ำ = ยกเลิกโหวต, เลือกข้ออื่น = เปลี่ยนโหวต
    // คืนค่า null เมื่อเป็นการยกเลิกโหวต
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public PollVote castVote(Long pollId, Long optionId, Long memberId) {
        Poll poll = findActivePoll(pollId, "โหวตนี้ถูกปิดรับคะแนนแล้ว");
        requireTripMember(poll.getTripId(), memberId);

        PollOption option = pollOptionRepo.findById(optionId)
                .orElseThrow(() -> badRequest("ไม่พบตัวเลือกนี้"));
        if (!pollId.equals(option.getPollId())) {
            throw badRequest("ตัวเลือกนี้ไม่ได้อยู่ในหัวข้อโหวตนี้");
        }

        Optional<PollVote> existing = pollVoteRepo.findByPollIdAndMemberId(pollId, memberId);
        if (existing.isPresent()) {
            PollVote vote = existing.get();
            if (optionId.equals(vote.getOptionId())) {
                pollVoteRepo.delete(vote);
                return null;
            }
            vote.setOptionId(optionId);
            return pollVoteRepo.save(vote);
        }

        PollVote vote = new PollVote();
        vote.setPollId(pollId);
        vote.setOptionId(optionId);
        vote.setMemberId(memberId);
        return pollVoteRepo.save(vote);
    }

    public List<PollResultDTO> getPollResults(Long pollId) {
        Poll poll = pollRepo.findById(pollId)
                .orElseThrow(() -> badRequest("ไม่พบหัวข้อโหวตนี้"));
        return buildResults(poll, pollVoteRepo.findByPollId(pollId), memberNames(poll.getTripId()));
    }

    // โหวตทั้งหมดในทริป (ใหม่สุดก่อน) พร้อมผลคะแนนและข้อที่สมาชิกคนนี้เลือก
    @Transactional
    public List<PollSummaryDTO> getTripPolls(Long tripId, Long memberId) {
        Map<Long, String> names = memberNames(tripId);
        List<PollSummaryDTO> result = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now(clock);

        for (Poll poll : pollRepo.findByTripIdOrderByIdDesc(tripId)) {
            closeIfExpired(poll);
            Long remainingSeconds = poll.getClosesAt() == null ? null
                    : Math.max(0, (Duration.between(now, poll.getClosesAt()).toMillis() + 999) / 1000);
            List<PollVote> votes = pollVoteRepo.findByPollId(poll.getId());
            Long myOptionId = memberId == null ? null
                    : votes.stream()
                            .filter(v -> memberId.equals(v.getMemberId()))
                            .map(PollVote::getOptionId)
                            .findFirst()
                            .orElse(null);

            result.add(new PollSummaryDTO(
                    poll.getId(),
                    poll.getQuestion(),
                    poll.getStatus(),
                    poll.getCreatedByMemberId(),
                    names.get(poll.getCreatedByMemberId()),
                    poll.getCreatedAt(),
                    poll.getClosesAt(),
                    remainingSeconds,
                    votes.size(),
                    myOptionId,
                    buildResults(poll, votes, names)));
        }
        return result;
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public PollOption addOptionToPoll(Long pollId, String newOptionText, Long memberId) {
        Poll poll = findActivePoll(pollId, "ไม่สามารถเสนอตัวเลือกเพิ่มได้ เพราะโหวตถูกปิดแล้ว");
        requireTripMember(poll.getTripId(), memberId);

        String text = cleanOption(newOptionText);
        List<PollOption> options = pollOptionRepo.findByPollId(pollId);
        if (options.size() >= MAX_OPTIONS) {
            throw badRequest("ตัวเลือกเต็มแล้ว (สูงสุด " + MAX_OPTIONS + " ข้อ)");
        }
        boolean duplicate = options.stream().anyMatch(o -> o.getOptionText().equalsIgnoreCase(text));
        if (duplicate) {
            throw badRequest("มีตัวเลือกนี้อยู่แล้ว");
        }

        PollOption option = new PollOption();
        option.setPollId(pollId);
        option.setOptionText(text);
        return pollOptionRepo.save(option);
    }

    @Transactional
    public Poll createPoll(PollRequest request) {
        if (request == null || request.getTripId() == null) {
            throw badRequest("กรุณาระบุทริป");
        }
        String question = request.getQuestion() == null ? "" : request.getQuestion().trim();
        if (question.isEmpty()) {
            throw badRequest("กรุณาตั้งคำถาม");
        }
        if (question.length() > MAX_QUESTION_LENGTH) {
            throw badRequest("คำถามยาวเกิน " + MAX_QUESTION_LENGTH + " ตัวอักษร");
        }
        requireTripMember(request.getTripId(), request.getMemberId());

        // ตัดช่องว่างและข้อซ้ำ (ไม่สนตัวพิมพ์เล็กใหญ่) ออก โดยคงลำดับเดิม
        Map<String, String> unique = new LinkedHashMap<>();
        for (String raw : Optional.ofNullable(request.getOptions()).orElse(List.of())) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String text = cleanOption(raw);
            unique.putIfAbsent(text.toLowerCase(Locale.ROOT), text);
        }
        if (unique.size() < MIN_OPTIONS) {
            throw badRequest("ต้องมีตัวเลือกที่ไม่ซ้ำกันอย่างน้อย " + MIN_OPTIONS + " ข้อ");
        }
        if (unique.size() > MAX_OPTIONS) {
            throw badRequest("ตัวเลือกได้สูงสุด " + MAX_OPTIONS + " ข้อ");
        }
        Integer duration = request.getDurationMinutes();
        if (duration != null && (duration < 1 || duration > MAX_DURATION_MINUTES)) {
            throw badRequest("ระยะเวลาโหวตต้องอยู่ระหว่าง 1 นาที ถึง 30 วัน");
        }

        Poll poll = new Poll();
        poll.setTripId(request.getTripId());
        poll.setQuestion(question);
        poll.setStatus("ACTIVE"); // เปิดโหวตทันทีที่สร้าง
        poll.setCreatedByMemberId(request.getMemberId());
        LocalDateTime now = LocalDateTime.now(clock);
        poll.setCreatedAt(now);
        poll.setClosesAt(duration == null ? null : now.plusMinutes(duration));
        Poll saved = pollRepo.save(poll);

        for (String text : unique.values()) {
            PollOption option = new PollOption();
            option.setPollId(saved.getId());
            option.setOptionText(text);
            pollOptionRepo.save(option);
        }
        return saved;
    }

    // ปิดโหวต: เฉพาะคนสร้าง (โหวตเก่าที่ไม่มีคนสร้าง สมาชิกในทริปปิดได้)
    @Transactional
    public Poll closePoll(Long pollId, Long memberId) {
        Poll poll = pollRepo.findById(pollId)
                .orElseThrow(() -> badRequest("ไม่พบหัวข้อโหวตนี้"));
        requireOwner(poll, memberId, "เฉพาะคนสร้างโหวตเท่านั้นที่ปิดโหวตได้");
        poll.setStatus("CLOSED");
        return pollRepo.save(poll);
    }

    @Transactional
    public void deletePoll(Long pollId, Long memberId) {
        Poll poll = pollRepo.findById(pollId)
                .orElseThrow(() -> badRequest("ไม่พบหัวข้อโหวตนี้"));
        requireOwner(poll, memberId, "เฉพาะคนสร้างโหวตเท่านั้นที่ลบได้");
        pollVoteRepo.deleteAll(pollVoteRepo.findByPollId(pollId));
        pollOptionRepo.deleteAll(pollOptionRepo.findByPollId(pollId));
        pollRepo.delete(poll);
    }

    private List<PollResultDTO> buildResults(Poll poll, List<PollVote> votes, Map<Long, String> names) {
        Map<Long, List<String>> votersByOption = votes.stream()
                .collect(Collectors.groupingBy(
                        PollVote::getOptionId,
                        Collectors.mapping(v -> names.getOrDefault(v.getMemberId(), "สมาชิก #" + v.getMemberId()),
                                Collectors.toList())));

        return pollOptionRepo.findByPollIdOrderByIdAsc(poll.getId()).stream()
                .map(o -> new PollResultDTO(o.getId(), o.getOptionText(),
                        votersByOption.getOrDefault(o.getId(), List.of())))
                .collect(Collectors.toList());
    }

    private Map<Long, String> memberNames(Long tripId) {
        Map<Long, String> names = new HashMap<>();
        for (TripMember m : tripMemberRepo.findByTripId(tripId)) {
            names.put(m.getId(), m.getGuestName());
        }
        return names;
    }

    private Poll findActivePoll(Long pollId, String closedMessage) {
        Poll poll = pollRepo.findById(pollId)
                .orElseThrow(() -> badRequest("ไม่พบหัวข้อโหวตนี้"));
        if (closeIfExpired(poll)) {
            throw badRequest("หมดเวลาโหวตแล้ว");
        }
        if ("CLOSED".equals(poll.getStatus())) {
            throw badRequest(closedMessage);
        }
        return poll;
    }

    // ถึงเวลาปิดแล้วแต่ยังเปิดอยู่ -> ปิดให้เลย คืนค่า true ถ้าเพิ่งปิดตอนนี้
    private boolean closeIfExpired(Poll poll) {
        boolean expired = "ACTIVE".equals(poll.getStatus())
                && poll.getClosesAt() != null
                && !LocalDateTime.now(clock).isBefore(poll.getClosesAt());
        if (expired) {
            poll.setStatus("CLOSED");
            pollRepo.save(poll);
        }
        return expired;
    }

    private void requireTripMember(Long tripId, Long memberId) {
        if (memberId == null) {
            throw badRequest("กรุณาระบุสมาชิก");
        }
        TripMember member = tripMemberRepo.findById(memberId)
                .orElseThrow(() -> badRequest("ไม่พบสมาชิกนี้"));
        if (member.getTrip() == null || !tripId.equals(member.getTrip().getId())) {
            throw badRequest("สมาชิกนี้ไม่ได้อยู่ในทริปนี้");
        }
    }

    private void requireOwner(Poll poll, Long memberId, String message) {
        requireTripMember(poll.getTripId(), memberId);
        if (poll.getCreatedByMemberId() != null && !poll.getCreatedByMemberId().equals(memberId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        }
    }

    private String cleanOption(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) {
            throw badRequest("กรุณากรอกตัวเลือก");
        }
        if (text.length() > MAX_OPTION_LENGTH) {
            throw badRequest("ตัวเลือกยาวเกิน " + MAX_OPTION_LENGTH + " ตัวอักษร");
        }
        return text;
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
