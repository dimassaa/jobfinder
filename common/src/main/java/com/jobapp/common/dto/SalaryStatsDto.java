package com.jobapp.common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SalaryStatsDto {
    private List<ChartDataPointDto> dataPoints;
    private String period;
    private Double average;

    @JsonCreator
    public SalaryStatsDto(
            @JsonProperty("dataPoints") List<ChartDataPointDto> dataPoints,
            @JsonProperty("period") String period,
            @JsonProperty("average") Double average) {
        this.dataPoints = dataPoints;
        this.period = period;
        this.average = average;
    }

    public SalaryStatsDto() {}

    public List<ChartDataPointDto> getDataPoints() { return dataPoints; }
    public void setDataPoints(List<ChartDataPointDto> dataPoints) { this.dataPoints = dataPoints; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public Double getAverage() { return average; }
    public void setAverage(Double average) { this.average = average; }
}