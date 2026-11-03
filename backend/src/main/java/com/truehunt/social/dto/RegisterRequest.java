package com.truehunt.social.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RegisterRequest {
    private String name;
    private String fullName;

    @Email(message = "Invalid email format")
    private String email;

    private String phone;
    private String phoneNumber;
    private String professionalPhone;
    private String password;
    private String role; // "TALENT" or "PRODUCER" / "STUDIO"
    private String primaryCraft;
    private String craft;
    private String subCraft;
    private String experience;
    private String experienceYears;
    private String companyName;
    private String headquarters;
    private String otp;
    private String emailOtp;
    private Boolean acceptedTerms = true;
    private List<String> secondaryCrafts;
    private List<String> secondarySubCrafts;
    private List<String> languages;

    public RegisterRequest() {}

    public String getName() {
        return (name != null && !name.isBlank()) ? name : fullName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return (phone != null && !phone.isBlank()) ? phone : phoneNumber;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getProfessionalPhone() {
        return professionalPhone;
    }

    public void setProfessionalPhone(String professionalPhone) {
        this.professionalPhone = professionalPhone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPrimaryCraft() {
        return (primaryCraft != null && !primaryCraft.isBlank()) ? primaryCraft : craft;
    }

    public void setPrimaryCraft(String primaryCraft) {
        this.primaryCraft = primaryCraft;
    }

    public String getCraft() {
        return craft;
    }

    public void setCraft(String craft) {
        this.craft = craft;
    }

    public String getSubCraft() {
        return subCraft;
    }

    public void setSubCraft(String subCraft) {
        this.subCraft = subCraft;
    }

    public String getExperience() {
        return (experience != null && !experience.isBlank()) ? experience : experienceYears;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public String getExperienceYears() {
        return experienceYears;
    }

    public void setExperienceYears(String experienceYears) {
        this.experienceYears = experienceYears;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getHeadquarters() {
        return headquarters;
    }

    public void setHeadquarters(String headquarters) {
        this.headquarters = headquarters;
    }

    public String getOtp() {
        return otp != null ? otp : emailOtp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public String getEmailOtp() {
        return emailOtp;
    }

    public void setEmailOtp(String emailOtp) {
        this.emailOtp = emailOtp;
    }

    public Boolean getAcceptedTerms() {
        return acceptedTerms != null ? acceptedTerms : true;
    }

    public void setAcceptedTerms(Boolean acceptedTerms) {
        this.acceptedTerms = acceptedTerms;
    }

    public List<String> getSecondaryCrafts() {
        return secondaryCrafts;
    }

    public void setSecondaryCrafts(List<String> secondaryCrafts) {
        this.secondaryCrafts = secondaryCrafts;
    }

    public List<String> getSecondarySubCrafts() {
        return secondarySubCrafts;
    }

    public void setSecondarySubCrafts(List<String> secondarySubCrafts) {
        this.secondarySubCrafts = secondarySubCrafts;
    }

    public List<String> getLanguages() {
        return languages;
    }

    public void setLanguages(List<String> languages) {
        this.languages = languages;
    }
}
