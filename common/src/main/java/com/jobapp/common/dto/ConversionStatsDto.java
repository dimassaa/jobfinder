package com.jobapp.common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ConversionStatsDto {
    private Integer views;
    private Integer responses;
    private Integer invites;
    private Double conversionRate;

    @JsonCreator
    public ConversionStatsDto(
            @JsonProperty("views") Integer views,
            @JsonProperty("responses") Integer responses,
            @JsonProperty("invites") Integer invites,
            @JsonProperty("conversionRate") Double conversionRate) {
        this.views = views;
        this.responses = responses;
        this.invites = invites;
        this.conversionRate = conversionRate;
    }

    public ConversionStatsDto() {}

    public Integer getViews() { return views; }
    public void setViews(Integer views) { this.views = views; }

    public Integer getResponses() { return responses; }
    public void setResponses(Integer responses) { this.responses = responses; }

    public Integer getInvites() { return invites; }
    public void setInvites(Integer invites) { this.invites = invites; }

    public Double getConversionRate() { return conversionRate; }
    public void setConversionRate(Double conversionRate) { this.conversionRate = conversionRate; }
}