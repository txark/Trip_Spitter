package com.cp.party_trip.dto;

import java.util.ArrayList;
import java.util.List;

public class PollResultDTO {
    private Long optionId;
    private String optionText;
    private int voteCount;
    private List<String> voters = new ArrayList<>(); // ชื่อสมาชิกที่เลือกข้อนี้

    public PollResultDTO(Long optionId, String optionText, int voteCount) {
        this.optionId = optionId;
        this.optionText = optionText;
        this.voteCount = voteCount;
    }

    public PollResultDTO(Long optionId, String optionText, List<String> voters) {
        this(optionId, optionText, voters.size());
        this.voters = voters;
    }

    public Long getOptionId() {
        return optionId;
    }

    public void setOptionId(Long optionId) {
        this.optionId = optionId;
    }

    public String getOptionText() {
        return optionText;
    }

    public void setOptionText(String optionText) {
        this.optionText = optionText;
    }

    public int getVoteCount() {
        return voteCount;
    }

    public void setVoteCount(int voteCount) {
        this.voteCount = voteCount;
    }

    public List<String> getVoters() {
        return voters;
    }

    public void setVoters(List<String> voters) {
        this.voters = voters;
    }
}
