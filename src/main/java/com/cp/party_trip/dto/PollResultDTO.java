package com.cp.party_trip.dto;

public class PollResultDTO {
    private Long optionId;
    private String optionText;
    private int voteCount;

    public PollResultDTO(Long optionId, String optionText, int voteCount) {
        this.optionId = optionId;
        this.optionText = optionText;
        this.voteCount = voteCount;
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
}