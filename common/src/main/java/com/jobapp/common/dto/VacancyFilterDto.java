package com.jobapp.common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class VacancyFilterDto {

    private String city;
    private Integer salaryFrom;
    private Integer salaryTo;
    private String employmentType;
    private List<Integer> tagIds;
    private String keyword;
    private LocalDateTime createdAfter;

    @JsonCreator
    public VacancyFilterDto(
            @JsonProperty("city") String city,
            @JsonProperty("salaryFrom") Integer salaryFrom,
            @JsonProperty("salaryTo") Integer salaryTo,
            @JsonProperty("employmentType") String employmentType,
            @JsonProperty("tagIds") List<Integer> tagIds,
            @JsonProperty("keyword") String keyword,
            @JsonProperty("createdAfter") LocalDateTime createdAfter) {
        this.city = city;
        this.salaryFrom = salaryFrom;
        this.salaryTo = salaryTo;
        this.employmentType = employmentType;
        this.tagIds = tagIds;
        this.keyword = keyword;
        this.createdAfter = createdAfter;
    }

    public VacancyFilterDto() {}

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public Integer getSalaryFrom() { return salaryFrom; }
    public void setSalaryFrom(Integer salaryFrom) { this.salaryFrom = salaryFrom; }

    public Integer getSalaryTo() { return salaryTo; }
    public void setSalaryTo(Integer salaryTo) { this.salaryTo = salaryTo; }

    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }

    public List<Integer> getTagIds() { return tagIds; }
    public void setTagIds(List<Integer> tagIds) { this.tagIds = tagIds; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public LocalDateTime getCreatedAfter() { return createdAfter; }
    public void setCreatedAfter(LocalDateTime createdAfter) { this.createdAfter = createdAfter; }
}