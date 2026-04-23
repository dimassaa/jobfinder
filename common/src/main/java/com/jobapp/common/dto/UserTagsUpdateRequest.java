package com.jobapp.common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class UserTagsUpdateRequest {

    private List<Integer> tagIds;

    @JsonCreator
    public UserTagsUpdateRequest(
            @JsonProperty("tagIds") List<Integer> tagIds) {
        this.tagIds = tagIds;
    }

    public UserTagsUpdateRequest() {}

    public List<Integer> getTagIds() { return tagIds; }
    public void setTagIds(List<Integer> tagIds) { this.tagIds = tagIds; }
}