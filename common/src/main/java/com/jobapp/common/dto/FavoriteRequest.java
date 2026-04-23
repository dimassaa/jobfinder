package com.jobapp.common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FavoriteRequest {

    private Integer vacancyId;

    @JsonCreator
    public FavoriteRequest(@JsonProperty("vacancyId") Integer vacancyId) {
        this.vacancyId = vacancyId;
    }

    public FavoriteRequest() {}

    public Integer getVacancyId() { return vacancyId; }
    public void setVacancyId(Integer vacancyId) { this.vacancyId = vacancyId; }
}