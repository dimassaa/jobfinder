package com.jobapp.common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RegisterRequest {

    private String email;
    private String password;
    private String role;
    private String name;
    private String surname;

    @JsonCreator
    public RegisterRequest(
            @JsonProperty("email") String email,
            @JsonProperty("password") String password,
            @JsonProperty("role") String role,
            @JsonProperty("name") String name,
            @JsonProperty("surname") String surname) {
        this.email = email;
        this.password = password;
        this.role = role;
        this.name = name;
        this.surname = surname;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getRole() {
        return role;
    }

    public String getName() {
        return name;
    }

    public String getSurname() {
        return surname;
    }
}