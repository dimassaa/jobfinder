package com.jobapp.common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApplicationDto {

    private Integer id;
    private Integer vacancyId;
    private String vacancyTitle;
    private Integer userId;
    private String userName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @JsonCreator
    public ApplicationDto(
            @JsonProperty("id") Integer id,
            @JsonProperty("vacancyId") Integer vacancyId,
            @JsonProperty("vacancyTitle") String vacancyTitle,
            @JsonProperty("userId") Integer userId,
            @JsonProperty("userName") String userName,
            @JsonProperty("status") String status,
            @JsonProperty("createdAt") LocalDateTime createdAt,
            @JsonProperty("updatedAt") LocalDateTime updatedAt) {
        this.id = id;
        this.vacancyId = vacancyId;
        this.vacancyTitle = vacancyTitle;
        this.userId = userId;
        this.userName = userName;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public ApplicationDto() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getVacancyId() { return vacancyId; }
    public void setVacancyId(Integer vacancyId) { this.vacancyId = vacancyId; }

    public String getVacancyTitle() { return vacancyTitle; }
    public void setVacancyTitle(String vacancyTitle) { this.vacancyTitle = vacancyTitle; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}