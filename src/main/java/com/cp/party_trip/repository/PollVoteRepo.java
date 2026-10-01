package com.cp.party_trip.repository;

import com.cp.party_trip.model.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PollVoteRepo extends JpaRepository<PollVote, Long> {
    boolean existsByPollIdAndMemberId(Long pollId, Long memberId);

    Optional<PollVote> findByPollIdAndMemberId(Long pollId, Long memberId);

    List<PollVote> findByPollId(Long pollId);

    int countByOptionId(Long optionId); // สำหรับนับคะแนนโหวตแบบ Real-time
}
