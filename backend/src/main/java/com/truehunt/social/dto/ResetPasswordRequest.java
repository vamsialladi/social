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
public class ResetPasswordRequest {

    @NotBlank(message = "Reset token is required")
    @JsonAlias({"resetToken", "token"})
    private String token;

    @NotBlank(message = "Password is required")
    @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH,
            message = "Password must be between 8 and 20 characters")
    @Pattern(regexp = PasswordPolicy.NO_SPACES_REGEX, message = "Password must not contain spaces")
    @ValidPassword
    @JsonAlias({"password", "new-password", "newPassword"})
    private String newPassword;

    @NotBlank(message = "Confirm password is required")
    @JsonAlias({"confirm-password", "confirmPassword"})
    private String confirmPassword;

    public ResetPasswordRequest() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
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

