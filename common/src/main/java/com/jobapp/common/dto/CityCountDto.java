package com.jobapp.common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CityCountDto {
    private String city;
    private Integer count;
    private Double avgSalary;

    @JsonCreator
    public CityCountDto(
            @JsonProperty("city") String city,
            @JsonProperty("count") Integer count,
            @JsonProperty("avgSalary") Double avgSalary) {
        this.city = city;
        this.count = count;
        this.avgSalary = avgSalary;
    }

    public CityCountDto() {}

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }

    public Double getAvgSalary() { return avgSalary; }
    public void setAvgSalary(Double avgSalary) { this.avgSalary = avgSalary; }
}