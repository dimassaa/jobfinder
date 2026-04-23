package com.jobapp.common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ComplaintCreateRequest {

    private String targetType;
    private Integer targetId;
    private String reason;

    @JsonCreator
    public ComplaintCreateRequest(
            @JsonProperty("targetType") String targetType,
            @JsonProperty("targetId") Integer targetId,
            @JsonProperty("reason") String reason) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.reason = reason;
    }

    public ComplaintCreateRequest() {}

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public Integer getTargetId() { return targetId; }
    public void setTargetId(Integer targetId) { this.targetId = targetId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}