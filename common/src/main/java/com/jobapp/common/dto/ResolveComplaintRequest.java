package com.jobapp.common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ResolveComplaintRequest {

    private Integer complaintId;
    private String action;

    @JsonCreator
    public ResolveComplaintRequest(
            @JsonProperty("complaintId") Integer complaintId,
            @JsonProperty("action") String action) {
        this.complaintId = complaintId;
        this.action = action;
    }

    public ResolveComplaintRequest() {}

    public Integer getComplaintId() { return complaintId; }
    public void setComplaintId(Integer complaintId) { this.complaintId = complaintId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
}
