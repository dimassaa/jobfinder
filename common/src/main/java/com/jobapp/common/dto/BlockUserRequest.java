package com.jobapp.common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BlockUserRequest {

    private Integer userId;
    private String reason;
    private Integer durationHours;

    @JsonCreator
    public BlockUserRequest(
            @JsonProperty("userId") Integer userId,
            @JsonProperty("reason") String reason,
            @JsonProperty("durationHours") Integer durationHours) {
        this.userId = userId;
        this.reason = reason;
        this.durationHours = durationHours;
    }

    public BlockUserRequest() {}

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Integer getDurationHours() { return durationHours; }
    public void setDurationHours(Integer durationHours) { this.durationHours = durationHours; }
}
