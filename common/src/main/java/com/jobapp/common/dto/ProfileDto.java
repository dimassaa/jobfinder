package com.jobapp.common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfileDto {

    private String name;
    private String surname;
    private String city;
    private String phone;
    private String additionalInfo;

    private Integer companyId;
    private String companyName;
    private String companyWebsite;
    private String companyDescription;
    private List<TagDto> tags;

    @JsonCreator
    public ProfileDto(
            @JsonProperty("name") String name,
            @JsonProperty("surname") String surname,
            @JsonProperty("city") String city,
            @JsonProperty("phone") String phone,
            @JsonProperty("additionalInfo") String additionalInfo) {
        this.name = name;
        this.surname = surname;
        this.city = city;
        this.phone = phone;
        this.additionalInfo = additionalInfo;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAdditionalInfo() { return additionalInfo; }
    public void setAdditionalInfo(String additionalInfo) { this.additionalInfo = additionalInfo; }

    public Integer getCompanyId() { return companyId; }
    public void setCompanyId(Integer companyId) { this.companyId = companyId; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getCompanyWebsite() { return companyWebsite; }
    public void setCompanyWebsite(String companyWebsite) { this.companyWebsite = companyWebsite; }

    public String getCompanyDescription() { return companyDescription; }
    public void setCompanyDescription(String companyDescription) { this.companyDescription = companyDescription; }

    public List<TagDto> getTags() { return tags; }
    public void setTags(List<TagDto> tags) { this.tags = tags; }
}