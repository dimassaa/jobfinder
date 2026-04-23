package com.jobapp.common.dto;

public class VacancyUpdateRequest {
    private Integer vacancyId;
    private VacancyCreateRequest vacancy;

    public VacancyUpdateRequest() {}

    public Integer getVacancyId() { return vacancyId; }
    public void setVacancyId(Integer vacancyId) { this.vacancyId = vacancyId; }

    public VacancyCreateRequest getVacancy() { return vacancy; }
    public void setVacancy(VacancyCreateRequest vacancy) { this.vacancy = vacancy; }
}