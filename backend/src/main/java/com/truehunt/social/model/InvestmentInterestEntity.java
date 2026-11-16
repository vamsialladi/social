package com.truehunt.social.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "investment_interests")
public class InvestmentInterestEntity {

    @Id
    private String id;

    private String investorId;
    private String investorName;
    private String projectTitle;
    private String producerName;
    private String producerEmail;
    private String budgetRequested;
    private String lookbookUrl;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String synopsis;

    private String status = "SUBMITTED";
    private LocalDateTime submittedAt = LocalDateTime.now();

    public InvestmentInterestEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getInvestorId() { return investorId; }
    public void setInvestorId(String investorId) { this.investorId = investorId; }

    public String getInvestorName() { return investorName; }
    public void setInvestorName(String investorName) { this.investorName = investorName; }

    public String getProjectTitle() { return projectTitle; }
    public void setProjectTitle(String projectTitle) { this.projectTitle = projectTitle; }

    public String getProducerName() { return producerName; }
    public void setProducerName(String producerName) { this.producerName = producerName; }

    public String getProducerEmail() { return producerEmail; }
    public void setProducerEmail(String producerEmail) { this.producerEmail = producerEmail; }

    public String getBudgetRequested() { return budgetRequested; }
    public void setBudgetRequested(String budgetRequested) { this.budgetRequested = budgetRequested; }

    public String getLookbookUrl() { return lookbookUrl; }
    public void setLookbookUrl(String lookbookUrl) { this.lookbookUrl = lookbookUrl; }

    public String getSynopsis() { return synopsis; }
    public void setSynopsis(String synopsis) { this.synopsis = synopsis; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
}
