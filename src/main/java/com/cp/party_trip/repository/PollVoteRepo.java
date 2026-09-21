package com.cp.party_trip.repository;

import com.cp.party_trip.model.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PollVoteRepo extends JpaRepository<PollVote, Long> {
    boolean existsByPollIdAndMemberId(Long pollId, Long memberId);

    int countByOptionId(Long optionId); // สำหรับนับคะแนนโหวตแบบ Real-time
}