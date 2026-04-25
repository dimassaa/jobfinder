package com.jobapp.server.model;

import java.time.LocalDateTime;

public class BlockHistory {
    private Integer id;
    private Integer userId;
    private Integer blockedBy;
    private String reason;
    private LocalDateTime blockedUntil;
    private LocalDateTime createdAt;

    public BlockHistory() {}

    public BlockHistory(Integer userId, Integer blockedBy, String reason, LocalDateTime blockedUntil) {
        this.userId = userId;
        this.blockedBy = blockedBy;
        this.reason = reason;
        this.blockedUntil = blockedUntil;
        this.createdAt = LocalDateTime.now();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getBlockedBy() { return blockedBy; }
    public void setBlockedBy(Integer blockedBy) { this.blockedBy = blockedBy; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getBlockedUntil() { return blockedUntil; }
    public void setBlockedUntil(LocalDateTime blockedUntil) { this.blockedUntil = blockedUntil; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}