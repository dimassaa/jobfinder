package com.jobapp.server.model;

import java.time.LocalDateTime;

public class Tag {
    private Integer id;
    private String name;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public Tag() {}

    public Tag(String name) {
        this.name = name;
        this.isActive = true;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}