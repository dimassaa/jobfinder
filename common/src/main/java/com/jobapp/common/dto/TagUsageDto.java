package com.jobapp.common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TagUsageDto {
    private Integer id;
    private String name;
    private Integer count;

    @JsonCreator
    public TagUsageDto(
            @JsonProperty("id") Integer id,
            @JsonProperty("name") String name,
            @JsonProperty("count") Integer count) {
        this.id = id;
        this.name = name;
        this.count = count;
    }

    public TagUsageDto() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
}