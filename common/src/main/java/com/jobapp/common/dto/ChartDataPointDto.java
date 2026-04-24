package com.jobapp.common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ChartDataPointDto {
    private String label;
    private double value;

    @JsonCreator
    public ChartDataPointDto(
            @JsonProperty("label") String label,
            @JsonProperty("value") double value) {
        this.label = label;
        this.value = value;
    }

    public ChartDataPointDto() {}

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
}