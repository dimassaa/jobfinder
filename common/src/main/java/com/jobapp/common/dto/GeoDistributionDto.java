package com.jobapp.common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class GeoDistributionDto {
    private List<CityCountDto> distribution;

    @JsonCreator
    public GeoDistributionDto(
            @JsonProperty("distribution") List<CityCountDto> distribution) {
        this.distribution = distribution;
    }

    public GeoDistributionDto() {}

    public List<CityCountDto> getDistribution() { return distribution; }
    public void setDistribution(List<CityCountDto> distribution) { this.distribution = distribution; }
}