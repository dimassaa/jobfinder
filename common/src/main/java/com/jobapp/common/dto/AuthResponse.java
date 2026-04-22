package com.jobapp.common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthResponse {

    private boolean success;
    private String message;
    private Integer userId;
    private String role;

    @JsonCreator
    public AuthResponse(
            @JsonProperty("success") boolean success,
            @JsonProperty("message") String message,
            @JsonProperty("userId") Integer userId,
            @JsonProperty("role") String role) {
        this.success = success;
        this.message = message;
        this.userId = userId;
        this.role = role;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Integer getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }
}