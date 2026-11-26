package com.truehunt.social.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "opportunity_applications",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_opportunity_applicant",
                columnNames = {"post_id", "applicant_identity"}
        )
)
public class OpportunityApplicationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "applicant_identity", nullable = false, length = 255)
    private String applicantIdentity;

    @Column(name = "applicant_name", length = 255)
    private String applicantName;

    @Column(name = "experience", length = 255)
    private String experience;

    @Lob
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "shortlisted", nullable = false)
    private boolean shortlisted;

    @Column(name = "shortlist_decision_made")
    private Boolean shortlistDecisionMade = false;

    private LocalDateTime appliedAt = LocalDateTime.now();

    public OpportunityApplicationEntity() {}

    public OpportunityApplicationEntity(Long postId, String applicantIdentity) {
        this.postId = postId;
        this.applicantIdentity = applicantIdentity;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }

    public String getApplicantIdentity() { return applicantIdentity; }
    public void setApplicantIdentity(String applicantIdentity) { this.applicantIdentity = applicantIdentity; }

    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }

    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public boolean isShortlisted() { return shortlisted; }
    public void setShortlisted(boolean shortlisted) { this.shortlisted = shortlisted; }

    public Boolean getShortlistDecisionMade() { return shortlistDecisionMade; }
    public void setShortlistDecisionMade(Boolean shortlistDecisionMade) {
        this.shortlistDecisionMade = shortlistDecisionMade;
    }

    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
}
