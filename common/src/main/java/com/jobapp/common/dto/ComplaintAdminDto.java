package com.jobapp.common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ComplaintAdminDto extends ComplaintDto {

    private String targetName;

    @JsonCreator
    public ComplaintAdminDto(
            @JsonProperty("id") Integer id,
            @JsonProperty("userId") Integer userId,
            @JsonProperty("targetType") String targetType,
            @JsonProperty("targetId") Integer targetId,
            @JsonProperty("reason") String reason,
            @JsonProperty("status") String status,
            @JsonProperty("createdAt") LocalDateTime createdAt,
            @JsonProperty("resolvedBy") Integer resolvedBy,
            @JsonProperty("resolvedAt") LocalDateTime resolvedAt,
            @JsonProperty("targetName") String targetName) {
        super(id, userId, targetType, targetId, reason, status, createdAt, resolvedBy, resolvedAt);
        this.targetName = targetName;
    }

    public ComplaintAdminDto() {}

    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }
}
