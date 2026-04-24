package com.jobapp.common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SystemStatsDto {
    private Integer totalUsers;
    private Integer totalApplicants;
    private Integer totalHr;
    private Integer totalAdmins;
    private Integer totalVacancies;
    private Integer totalApplications;

    @JsonCreator
    public SystemStatsDto(
            @JsonProperty("totalUsers") Integer totalUsers,
            @JsonProperty("totalApplicants") Integer totalApplicants,
            @JsonProperty("totalHr") Integer totalHr,
            @JsonProperty("totalAdmins") Integer totalAdmins,
            @JsonProperty("totalVacancies") Integer totalVacancies,
            @JsonProperty("totalApplications") Integer totalApplications) {
        this.totalUsers = totalUsers;
        this.totalApplicants = totalApplicants;
        this.totalHr = totalHr;
        this.totalAdmins = totalAdmins;
        this.totalVacancies = totalVacancies;
        this.totalApplications = totalApplications;
    }

    public SystemStatsDto() {}

    public Integer getTotalUsers() { return totalUsers; }
    public void setTotalUsers(Integer totalUsers) { this.totalUsers = totalUsers; }

    public Integer getTotalApplicants() { return totalApplicants; }
    public void setTotalApplicants(Integer totalApplicants) { this.totalApplicants = totalApplicants; }

    public Integer getTotalHr() { return totalHr; }
    public void setTotalHr(Integer totalHr) { this.totalHr = totalHr; }

    public Integer getTotalAdmins() { return totalAdmins; }
    public void setTotalAdmins(Integer totalAdmins) { this.totalAdmins = totalAdmins; }

    public Integer getTotalVacancies() { return totalVacancies; }
    public void setTotalVacancies(Integer totalVacancies) { this.totalVacancies = totalVacancies; }

    public Integer getTotalApplications() { return totalApplications; }
    public void setTotalApplications(Integer totalApplications) { this.totalApplications = totalApplications; }
}