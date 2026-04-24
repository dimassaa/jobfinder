package com.jobapp.common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationDto {

    private Integer id;
    private Integer userId;
    private String type;
    private String title;
    private String content;
    private String link;
    private Boolean isRead;
    private LocalDateTime createdAt;

    @JsonCreator
    public NotificationDto(
            @JsonProperty("id") Integer id,
            @JsonProperty("userId") Integer userId,
            @JsonProperty("type") String type,
            @JsonProperty("title") String title,
            @JsonProperty("content") String content,
            @JsonProperty("link") String link,
            @JsonProperty("isRead") Boolean isRead,
            @JsonProperty("createdAt") LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.content = content;
        this.link = link;
        this.isRead = isRead;
        this.createdAt = createdAt;
    }

    public NotificationDto() {}

    public NotificationDto(String type, String title, String content, String link) {
        this.type = type;
        this.title = title;
        this.content = content;
        this.link = link;
        this.isRead = false;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}