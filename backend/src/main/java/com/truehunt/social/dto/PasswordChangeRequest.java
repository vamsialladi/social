package com.truehunt.social.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.truehunt.social.validation.PasswordMatches;
import com.truehunt.social.validation.PasswordPolicy;
import com.truehunt.social.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
@PasswordMatches(passwordField = "newPassword", confirmPasswordField = "confirmPassword")
public class PasswordChangeRequest {

    @NotBlank(message = "Current password is required")
    @JsonAlias({"current-password", "currentPassword"})
    private String currentPassword;

    @NotBlank(message = "Password is required")
    @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH,
            message = "Password must be between 8 and 20 characters")
    @Pattern(regexp = PasswordPolicy.NO_SPACES_REGEX, message = "Password must not contain spaces")
    @ValidPassword
    @JsonAlias({"new-password", "newPassword"})
    private String newPassword;

    @JsonAlias({"confirm-password", "confirmPassword"})
    private String confirmPassword;

    public PasswordChangeRequest() {}

    public PasswordChangeRequest(String currentPassword, String newPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
