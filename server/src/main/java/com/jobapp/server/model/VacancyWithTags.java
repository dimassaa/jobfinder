package com.jobapp.server.model;

import java.util.Set;

public class VacancyWithTags {
    private int vacancyId;
    private Set<Integer> tagIds;

    public VacancyWithTags() {}

    public VacancyWithTags(int vacancyId, Set<Integer> tagIds) {
        this.vacancyId = vacancyId;
        this.tagIds = tagIds;
    }

    public int getVacancyId() {
        return vacancyId;
    }

    public void setVacancyId(int vacancyId) {
        this.vacancyId = vacancyId;
    }

    public Set<Integer> getTagIds() {
        return tagIds;
    }

    public void setTagIds(Set<Integer> tagIds) {
        this.tagIds = tagIds;
    }
}