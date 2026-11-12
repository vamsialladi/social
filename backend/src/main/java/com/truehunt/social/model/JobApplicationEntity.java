package com.truehunt.social.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_applications")
public class JobApplicationEntity {

    @Id
    private String id;

    private String jobId;
    private String jobTitle;
    private String applicantName;
    private String applicantEmail;
    private String phone;
    private String portfolioLink;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String pitchNote;

    private String status = "SUBMITTED";
    private LocalDateTime submittedAt = LocalDateTime.now();

    public JobApplicationEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }

    public String getApplicantEmail() { return applicantEmail; }
    public void setApplicantEmail(String applicantEmail) { this.applicantEmail = applicantEmail; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPortfolioLink() { return portfolioLink; }
    public void setPortfolioLink(String portfolioLink) { this.portfolioLink = portfolioLink; }

    public String getPitchNote() { return pitchNote; }
    public void setPitchNote(String pitchNote) { this.pitchNote = pitchNote; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
}

