package com.truehunt.social.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JobApplicationRequest {
    @NotBlank(message = "Applicant name is required")
    private String applicantName;

    private String applicantEmail;
    @JsonAlias({"phone"})
    private String applicantPhone;
    private String experience;
    private String coverLetter;
    private String pitchNote;
    private String portfolioUrl;
    private String portfolioLink;
    private String resumeUrl;
    private String expectedPay;
    private String jobTitle;

    public JobApplicationRequest() {}

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public String getApplicantEmail() {
        return applicantEmail;
    }

    public void setApplicantEmail(String applicantEmail) {
        this.applicantEmail = applicantEmail;
    }

    public String getApplicantPhone() {
        return applicantPhone;
    }

    public void setApplicantPhone(String applicantPhone) {
        this.applicantPhone = applicantPhone;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public String getCoverLetter() {
        return coverLetter != null ? coverLetter : pitchNote;
    }

    public void setCoverLetter(String coverLetter) {
        this.coverLetter = coverLetter;
    }

    public String getPitchNote() {
        return pitchNote != null ? pitchNote : coverLetter;
    }

    public void setPitchNote(String pitchNote) {
        this.pitchNote = pitchNote;
    }

    public String getPortfolioUrl() {
        return portfolioUrl != null ? portfolioUrl : portfolioLink;
    }

    public void setPortfolioUrl(String portfolioUrl) {
        this.portfolioUrl = portfolioUrl;
    }

    public String getPortfolioLink() {
        return portfolioLink != null ? portfolioLink : portfolioUrl;
    }

    public void setPortfolioLink(String portfolioLink) {
        this.portfolioLink = portfolioLink;
    }

    public String getResumeUrl() {
        return resumeUrl;
    }

    public void setResumeUrl(String resumeUrl) {
        this.resumeUrl = resumeUrl;
    }

    public String getExpectedPay() {
        return expectedPay;
    }

    public void setExpectedPay(String expectedPay) {
        this.expectedPay = expectedPay;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }
}
