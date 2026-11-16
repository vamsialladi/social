package com.truehunt.social.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.truehunt.social.validation.PasswordMatches;
import com.truehunt.social.validation.PasswordPolicy;
import com.truehunt.social.validation.ValidPassword;
import com.truehunt.social.validation.ValidPhoneNumber;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@PasswordMatches
public class RegisterRequest {
    private String name;
    private String fullName;

    @NotBlank(message = "Email is required.")
    @Email(message = "Please enter a valid email address.")
    private String email;

    @ValidPhoneNumber
    private String phone;
    @ValidPhoneNumber
    private String phoneNumber;

    @NotBlank(message = "Password is required")
    @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH,
            message = "Password must be between 8 and 20 characters")
    @Pattern(regexp = PasswordPolicy.NO_SPACES_REGEX, message = "Password must not contain spaces")
    @ValidPassword
    private String password;

    @JsonAlias({"confirm-password", "confirmPassword"})
    private String confirmPassword;

    private String role; // "TALENT" or "PRODUCER" / "STUDIO"
    private String primaryCraft;
    private String craft;
    private String productionProfileType;
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
        this.email = email == null ? null : email.trim();
    }

    public String getPhone() {
        if (phone != null && !phone.isBlank()) {
            return phone;
        }
        if (phoneNumber != null && !phoneNumber.isBlank()) {
            return phoneNumber;
        }
        return phone != null ? phone : phoneNumber;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPrimaryCraft() {
        if (primaryCraft != null && !primaryCraft.isBlank()) return primaryCraft;
        if (craft != null && !craft.isBlank()) return craft;
        return productionProfileType;
    }

    public String getProductionProfileType() {
        return productionProfileType;
    }

    public void setProductionProfileType(String productionProfileType) {
        this.productionProfileType = productionProfileType;
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
