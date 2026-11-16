package com.truehunt.social.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InvestorAccessRequestDto {

    @NotBlank
    @Size(max = 120)
    private String name;

    @Size(max = 30)
    private String mobileNumber;

    @NotBlank
    @Size(max = 100)
    private String budget;

    @NotBlank
    @Size(max = 1000)
    private String description;

    @NotBlank
    @Pattern(regexp = "Producer|Investor|Crowd funder")
    private String role;

    @AssertTrue
    private boolean consentAccepted;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }
    public String getBudget() { return budget; }
    public void setBudget(String budget) { this.budget = budget; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public boolean isConsentAccepted() { return consentAccepted; }
    public void setConsentAccepted(boolean consentAccepted) { this.consentAccepted = consentAccepted; }
}
