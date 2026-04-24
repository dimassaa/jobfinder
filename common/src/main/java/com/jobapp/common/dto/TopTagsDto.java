package com.jobapp.common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class TopTagsDto {
    private List<TagUsageDto> tags;

    @JsonCreator
    public TopTagsDto(
            @JsonProperty("tags") List<TagUsageDto> tags) {
        this.tags = tags;
    }

    public TopTagsDto() {}

    public List<TagUsageDto> getTags() { return tags; }
    public void setTags(List<TagUsageDto> tags) { this.tags = tags; }
}