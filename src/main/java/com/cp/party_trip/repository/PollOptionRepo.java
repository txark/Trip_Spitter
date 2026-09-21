package com.cp.party_trip.repository;

import com.cp.party_trip.model.PollOption;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PollOptionRepo extends JpaRepository<PollOption, Long> {
    List<PollOption> findByPollId(Long pollId);
}