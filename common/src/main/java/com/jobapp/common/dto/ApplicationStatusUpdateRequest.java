package com.jobapp.common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ApplicationStatusUpdateRequest {

    private Integer applicationId;
    private String newStatus;

    @JsonCreator
    public ApplicationStatusUpdateRequest(
            @JsonProperty("applicationId") Integer applicationId,
            @JsonProperty("newStatus") String newStatus) {
        this.applicationId = applicationId;
        this.newStatus = newStatus;
    }

    public ApplicationStatusUpdateRequest() {}

    public Integer getApplicationId() { return applicationId; }
    public void setApplicationId(Integer applicationId) { this.applicationId = applicationId; }

    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }
}