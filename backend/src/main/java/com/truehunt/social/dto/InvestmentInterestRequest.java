package com.truehunt.social.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InvestmentInterestRequest {
    private String investorId;
    private String investorName;
    private String projectTitle;
    private String producerName;
    private String producerEmail;
    private String budgetRequested;
    private String lookbookUrl;
    private String synopsis;

    // Alternative aliases
    private String projectId;
    private String name;
    private String email;
    private String phone;
    private String amount;
    private String interestType;
    private String notes;

    public InvestmentInterestRequest() {}

    public String getInvestorId() {
        return investorId;
    }

    public void setInvestorId(String investorId) {
        this.investorId = investorId;
    }

    public String getInvestorName() {
        return investorName;
    }

    public void setInvestorName(String investorName) {
        this.investorName = investorName;
    }

    public String getProjectTitle() {
        return projectTitle;
    }

    public void setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
    }

    public String getProducerName() {
        return (producerName != null && !producerName.isBlank()) ? producerName : name;
    }

    public void setProducerName(String producerName) {
        this.producerName = producerName;
    }

    public String getProducerEmail() {
        return (producerEmail != null && !producerEmail.isBlank()) ? producerEmail : email;
    }

    public void setProducerEmail(String producerEmail) {
        this.producerEmail = producerEmail;
    }

    public String getBudgetRequested() {
        return (budgetRequested != null && !budgetRequested.isBlank()) ? budgetRequested : amount;
    }

    public void setBudgetRequested(String budgetRequested) {
        this.budgetRequested = budgetRequested;
    }

    public String getLookbookUrl() {
        return lookbookUrl;
    }

    public void setLookbookUrl(String lookbookUrl) {
        this.lookbookUrl = lookbookUrl;
    }

    public String getSynopsis() {
        return (synopsis != null && !synopsis.isBlank()) ? synopsis : notes;
    }

    public void setSynopsis(String synopsis) {
        this.synopsis = synopsis;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getName() {
        return (name != null && !name.isBlank()) ? name : producerName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return (email != null && !email.isBlank()) ? email : producerEmail;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAmount() {
        return (amount != null && !amount.isBlank()) ? amount : budgetRequested;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getInterestType() {
        return interestType;
    }

    public void setInterestType(String interestType) {
        this.interestType = interestType;
    }

    public String getNotes() {
        return (notes != null && !notes.isBlank()) ? notes : synopsis;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
