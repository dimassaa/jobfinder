package com.jobapp.common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecommendedVacancyDto extends VacancyDto {

    private Double matchPercent;

    @JsonCreator
    public RecommendedVacancyDto(
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
            @JsonProperty("tags") List<TagDto> tags,
            @JsonProperty("matchPercent") Double matchPercent) {
        super(id, companyId, companyName, title, description, city, salaryMin, salaryMax, employmentType, isActive, createdAt, tags);
        this.matchPercent = matchPercent;
    }

    public RecommendedVacancyDto() {}

    public RecommendedVacancyDto(VacancyDto vacancy, Double matchPercent) {
        super(vacancy.getId(), vacancy.getCompanyId(), vacancy.getCompanyName(), vacancy.getTitle(),
                vacancy.getDescription(), vacancy.getCity(), vacancy.getSalaryMin(), vacancy.getSalaryMax(),
                vacancy.getEmploymentType(), vacancy.getIsActive(), vacancy.getCreatedAt(), vacancy.getTags());
        this.matchPercent = matchPercent;
    }

    public Double getMatchPercent() { return matchPercent; }
    public void setMatchPercent(Double matchPercent) { this.matchPercent = matchPercent; }
}