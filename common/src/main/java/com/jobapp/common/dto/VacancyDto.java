package com.jobapp.common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class VacancyDto {

    private Integer id;
    private Integer companyId;
    private String companyName;
    private String title;
    private String description;
    private String city;
    private Integer salaryMin;
    private Integer salaryMax;
    private String employmentType;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private List<TagDto> tags;

    @JsonCreator
    public VacancyDto(
            @JsonProperty("id") Integer id,
            @JsonProperty("companyId") Integer companyId,
            @JsonProperty("companyName") String companyName,
            @JsonProperty("title") String title,
            @JsonProperty("description") String description,
            @JsonProperty("city") String city,
            @JsonProperty("salaryMin") Integer salaryMin,
            @JsonProperty("salaryMax") Integer salaryMax,
            @JsonProperty("employmentType") String employmentType,
            @JsonProperty("isActive") Boolean isActive,
            @JsonProperty("createdAt") LocalDateTime createdAt,
            @JsonProperty("tags") List<TagDto> tags) {
        this.id = id;
        this.companyId = companyId;
        this.companyName = companyName;
        this.title = title;
        this.description = description;
        this.city = city;
        this.salaryMin = salaryMin;
        this.salaryMax = salaryMax;
        this.employmentType = employmentType;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.tags = tags;
    }

    public VacancyDto() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getCompanyId() { return companyId; }
    public void setCompanyId(Integer companyId) { this.companyId = companyId; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

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

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<TagDto> getTags() { return tags; }
    public void setTags(List<TagDto> tags) { this.tags = tags; }

    @Override
    public String toString() {
        return title != null ? title : "";
    }
}