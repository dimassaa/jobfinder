package com.jobapp.common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdminUserDto {

    private Integer id;
    private String email;
    private String role;
    private String name;
    private String surname;
    private String city;
    private Boolean isBlocked;
    private LocalDateTime blockedUntil;
    private LocalDateTime createdAt;

    @JsonCreator
    public AdminUserDto(
            @JsonProperty("id") Integer id,
            @JsonProperty("email") String email,
            @JsonProperty("role") String role,
            @JsonProperty("name") String name,
            @JsonProperty("surname") String surname,
            @JsonProperty("city") String city,
            @JsonProperty("isBlocked") Boolean isBlocked,
            @JsonProperty("blockedUntil") LocalDateTime blockedUntil,
            @JsonProperty("createdAt") LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.name = name;
        this.surname = surname;
        this.city = city;
        this.isBlocked = isBlocked;
        this.blockedUntil = blockedUntil;
        this.createdAt = createdAt;
    }

    public AdminUserDto() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public Boolean getIsBlocked() { return isBlocked; }
    public void setIsBlocked(Boolean blocked) { isBlocked = blocked; }

    public LocalDateTime getBlockedUntil() { return blockedUntil; }
    public void setBlockedUntil(LocalDateTime blockedUntil) { this.blockedUntil = blockedUntil; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
