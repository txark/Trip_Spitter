package com.cp.party_trip.service.impl;

import com.cp.party_trip.dto.request.PollRequest;
import com.cp.party_trip.dto.response.PollSummaryResponse;
import com.cp.party_trip.model.*;
import com.cp.party_trip.repository.PollOptionRepo;
import com.cp.party_trip.repository.PollRepo;
import com.cp.party_trip.repository.PollVoteRepo;
import com.cp.party_trip.repository.TripMemberRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PollServiceImplTest {

    private PollRepo pollRepo;
    private PollOptionRepo pollOptionRepo;
    private PollVoteRepo pollVoteRepo;
    private TripMemberRepo tripMemberRepo;
    private PollServiceImpl service;

    private final Trip trip = new Trip();
    private final Trip otherTrip = new Trip();
    private final List<TripMember> tripMembers = new ArrayList<>();

    @BeforeEach
    void setUp() {
        pollRepo = mock(PollRepo.class);
        pollOptionRepo = mock(PollOptionRepo.class);
        pollVoteRepo = mock(PollVoteRepo.class);
        tripMemberRepo = mock(TripMemberRepo.class);
        service = new PollServiceImpl(pollRepo, pollOptionRepo, pollVoteRepo, tripMemberRepo);
        service.setClock(Clock.fixed(Instant.parse("2026-10-01T05:00:00Z"), ZoneId.of("Asia/Bangkok")));

        trip.setId(1L);
        otherTrip.setId(2L);
        member(10L, trip);
        member(11L, trip);
        member(99L, otherTrip);
        when(tripMemberRepo.findByTripId(1L)).thenReturn(tripMembers);

        when(pollRepo.save(any(Poll.class))).thenAnswer(inv -> {
            Poll p = inv.getArgument(0);
            if (p.getId() == null) p.setId(5L);
            return p;
        });
        when(pollVoteRepo.save(any(PollVote.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pollOptionRepo.save(any(PollOption.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private void member(Long id, Trip t) {
        TripMember m = new TripMember();
        m.setId(id);
        m.setTrip(t);
        m.setGuestName("m" + id);
        when(tripMemberRepo.findById(id)).thenReturn(Optional.of(m));
        if (t == trip) tripMembers.add(m);
    }

    private Poll poll(String status, Long createdBy) {
        Poll p = new Poll();
        p.setId(5L);
        p.setTripId(1L);
        p.setQuestion("ไปไหนดี");
        p.setStatus(status);
        p.setCreatedByMemberId(createdBy);
        when(pollRepo.findById(5L)).thenReturn(Optional.of(p));
        return p;
    }

    private PollOption option(Long id, Long pollId, String text) {
        PollOption o = new PollOption();
        o.setId(id);
        o.setPollId(pollId);
        o.setOptionText(text);
        when(pollOptionRepo.findById(id)).thenReturn(Optional.of(o));
        return o;
    }

    private PollVote vote(Long optionId, Long memberId) {
        PollVote v = new PollVote();
        v.setPollId(5L);
        v.setOptionId(optionId);
        v.setMemberId(memberId);
        return v;
    }

    private PollRequest request(String question, String... options) {
        PollRequest r = new PollRequest();
        r.setTripId(1L);
        r.setMemberId(10L);
        r.setQuestion(question);
        r.setOptions(List.of(options));
        return r;
    }

    private void assertBadRequest(Runnable action) {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, action::run);
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
    }

    @Test
    void createPollSavesTrimmedUniqueOptions() {
        Poll saved = service.createPoll(request("  ไปไหนดี  ", " ทะเล ", "ภูเขา", "ทะเล", "  "));

        assertEquals("ไปไหนดี", saved.getQuestion());
        assertEquals(10L, saved.getCreatedByMemberId());
        ArgumentCaptor<PollOption> captor = ArgumentCaptor.forClass(PollOption.class);
        verify(pollOptionRepo, times(2)).save(captor.capture());
        assertEquals(List.of("ทะเล", "ภูเขา"), captor.getAllValues().stream().map(PollOption::getOptionText).toList());
    }

    @Test
    void createPollRejectsBlankQuestionAndTooFewOptions() {
        assertBadRequest(() -> service.createPoll(request(" ", "ทะเล", "ภูเขา")));
        assertBadRequest(() -> service.createPoll(request("ไปไหนดี", "ทะเล", "ทะเล")));
        verify(pollRepo, never()).save(any());
    }

    @Test
    void voteChangesThenCancels() {
        poll("ACTIVE", 10L);
        option(1L, 5L, "ทะเล");
        option(2L, 5L, "ภูเขา");
        PollVote existing = vote(1L, 11L);
        when(pollVoteRepo.findByPollIdAndMemberId(5L, 11L)).thenReturn(Optional.of(existing));

        PollVote changed = service.castVote(5L, 2L, 11L);
        assertEquals(2L, changed.getOptionId());

        assertNull(service.castVote(5L, 2L, 11L));
        verify(pollVoteRepo).delete(existing);
    }

    @Test
    void voteRejectsClosedPollForeignOptionAndOutsider() {
        poll("ACTIVE", 10L);
        option(3L, 6L, "อื่น");
        assertBadRequest(() -> service.castVote(5L, 3L, 11L));
        assertBadRequest(() -> service.castVote(5L, 3L, 99L));

        poll("CLOSED", 10L);
        option(1L, 5L, "ทะเล");
        assertBadRequest(() -> service.castVote(5L, 1L, 11L));
        verify(pollVoteRepo, never()).save(any());
    }

    @Test
    void onlyCreatorCanClose() {
        poll("ACTIVE", 10L);
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service.closePoll(5L, 11L));
        assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());

        assertEquals("CLOSED", service.closePoll(5L, 10L).getStatus());
    }

    @Test
    void addOptionRejectsDuplicate() {
        poll("ACTIVE", 10L);
        PollOption sea = option(1L, 5L, "ทะเล");
        when(pollOptionRepo.findByPollId(5L)).thenReturn(List.of(sea));

        assertBadRequest(() -> service.addOptionToPoll(5L, " ทะเล ", 11L));
        assertEquals("ภูเขา", service.addOptionToPoll(5L, " ภูเขา ", 11L).getOptionText());
    }

    @Test
    void tripPollsIncludeVotersAndMyChoice() {
        Poll p = poll("ACTIVE", 10L);
        when(pollRepo.findByTripIdOrderByIdDesc(1L)).thenReturn(List.of(p));
        List<PollOption> options = List.of(option(1L, 5L, "ทะเล"), option(2L, 5L, "ภูเขา"));
        when(pollOptionRepo.findByPollIdOrderByIdAsc(5L)).thenReturn(options);
        when(pollVoteRepo.findByPollId(5L)).thenReturn(List.of(vote(1L, 10L), vote(1L, 11L)));

        PollSummaryResponse summary = service.getTripPolls(1L, 11L).get(0);

        assertEquals(2, summary.getTotalVotes());
        assertEquals(1L, summary.getMyOptionId());
        assertEquals("m10", summary.getCreatedByName());
        assertEquals(List.of("m10", "m11"), summary.getOptions().get(0).getVoters());
        assertEquals(0, summary.getOptions().get(1).getVoteCount());
    }

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 1, 12, 0);

    @Test
    void createPollWithDurationSetsClosingTime() {
        PollRequest r = request("ไปไหนดี", "ทะเล", "ภูเขา");
        r.setDurationMinutes(90);
        assertEquals(NOW.plusMinutes(90), service.createPoll(r).getClosesAt());

        r.setDurationMinutes(null);
        assertNull(service.createPoll(r).getClosesAt());

        r.setDurationMinutes(0);
        assertBadRequest(() -> service.createPoll(r));
    }

    @Test
    void expiredPollRejectsVoteAndIsClosed() {
        Poll p = poll("ACTIVE", 10L);
        p.setClosesAt(NOW.minusMinutes(1));
        option(1L, 5L, "ทะเล");

        assertBadRequest(() -> service.castVote(5L, 1L, 11L));
        assertEquals("CLOSED", p.getStatus());
        verify(pollVoteRepo, never()).save(any());
    }

    @Test
    void tripPollsReportRemainingSecondsAndAutoClose() {
        Poll open = poll("ACTIVE", 10L);
        open.setClosesAt(NOW.plusMinutes(5));
        Poll expired = new Poll();
        expired.setId(4L);
        expired.setTripId(1L);
        expired.setStatus("ACTIVE");
        expired.setClosesAt(NOW.minusSeconds(1));
        when(pollRepo.findByTripIdOrderByIdDesc(1L)).thenReturn(List.of(open, expired));

        List<PollSummaryResponse> result = service.getTripPolls(1L, 11L);

        assertEquals(300L, result.get(0).getRemainingSeconds());
        assertEquals("ACTIVE", result.get(0).getStatus());
        assertEquals(0L, result.get(1).getRemainingSeconds());
        assertEquals("CLOSED", result.get(1).getStatus());
    }
}
