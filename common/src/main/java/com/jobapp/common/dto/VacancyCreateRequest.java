package com.jobapp.common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class VacancyCreateRequest {

    private String title;
    private String description;
    private String city;
    private Integer salaryMin;
    private Integer salaryMax;
    private String employmentType;
    private List<Integer> tagIds;

    @JsonCreator
    public VacancyCreateRequest(
            @JsonProperty("title") String title,
            @JsonProperty("description") String description,
            @JsonProperty("city") String city,
            @JsonProperty("salaryMin") Integer salaryMin,
            @JsonProperty("salaryMax") Integer salaryMax,
            @JsonProperty("employmentType") String employmentType,
            @JsonProperty("tagIds") List<Integer> tagIds) {
        this.title = title;
        this.description = description;
        this.city = city;
        this.salaryMin = salaryMin;
        this.salaryMax = salaryMax;
        this.employmentType = employmentType;
        this.tagIds = tagIds;
    }

    public VacancyCreateRequest() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public Integer getSalaryMin() { return salaryMin; }
    public void setSalaryMin(Integer salaryMin) { this.salaryMin = salaryMin; }

    public Integer getSalaryMax() { return salaryMax; }
    public void setSalaryMax(Integer salaryMax) { this.salaryMax = salaryMax; }

    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }

    public List<Integer> getTagIds() { return tagIds; }
    public void setTagIds(List<Integer> tagIds) { this.tagIds = tagIds; }
}