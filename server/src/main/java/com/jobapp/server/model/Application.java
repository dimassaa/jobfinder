package com.jobapp.server.model;

import java.time.LocalDateTime;

public class Application {
    private Integer id;
    private Integer vacancyId;
    private Integer userId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String userName;
    private String userSurname;

    public Application() {}

    public Application(Integer vacancyId, Integer userId) {
        this.vacancyId = vacancyId;
        this.userId = userId;
        this.status = "PENDING";
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getVacancyId() { return vacancyId; }
    public void setVacancyId(Integer vacancyId) { this.vacancyId = vacancyId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserSurname() { return userSurname; }
    public void setUserSurname(String userSurname) { this.userSurname = userSurname; }
}