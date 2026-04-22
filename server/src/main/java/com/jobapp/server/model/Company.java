package com.jobapp.server.model;

import java.time.LocalDateTime;

public class Company {
    private Integer id;
    private Integer userId;
    private String companyName;
    private String website;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Company() {}

    public Company(Integer userId, String companyName) {
        this.userId = userId;
        this.companyName = companyName;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}