package com.truehunt.social.model;

import jakarta.persistence.*;

@Entity
@Table(name = "jobs")
public class JobEntity {

    @Id
    private String id;

    private String title;
    private String roleCategory;
    private String productionCompany;
    private String location;
    private String type;
    private String budgetOrPay;
    private String postedDate;
    private String deadline;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String requirementsJson = "[]";

    private String department;
    private Integer applicationsCount = 0;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String skillsJson = "[]";

    private Boolean verified = false;
    private String experience;

    public JobEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getRoleCategory() { return roleCategory; }
    public void setRoleCategory(String roleCategory) { this.roleCategory = roleCategory; }

    public String getProductionCompany() { return productionCompany; }
    public void setProductionCompany(String productionCompany) { this.productionCompany = productionCompany; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getBudgetOrPay() { return budgetOrPay; }
    public void setBudgetOrPay(String budgetOrPay) { this.budgetOrPay = budgetOrPay; }

    public String getPostedDate() { return postedDate; }
    public void setPostedDate(String postedDate) { this.postedDate = postedDate; }

    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRequirementsJson() { return requirementsJson; }
    public void setRequirementsJson(String requirementsJson) { this.requirementsJson = requirementsJson; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public Integer getApplicationsCount() { return applicationsCount; }
    public void setApplicationsCount(Integer applicationsCount) { this.applicationsCount = applicationsCount; }

    public String getSkillsJson() { return skillsJson; }
    public void setSkillsJson(String skillsJson) { this.skillsJson = skillsJson; }

    public Boolean getVerified() { return verified; }
    public void setVerified(Boolean verified) { this.verified = verified; }

    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }
}

