package com.jobapp.common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AddTagRequest {

    private String tagName;

    @JsonCreator
    public AddTagRequest(@JsonProperty("tagName") String tagName) {
        this.tagName = tagName;
    }

    public AddTagRequest() {}

    public String getTagName() { return tagName; }
    public void setTagName(String tagName) { this.tagName = tagName; }
}
