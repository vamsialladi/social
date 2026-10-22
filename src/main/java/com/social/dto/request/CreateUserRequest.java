package com.social.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateUserRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Name is required")
    @Size(max = 150, message = "Name must not exceed 150 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    private String email;

    private String headline;
    private String avatarUrl;
    private String bio;
    private String role;
    private String location;

    public CreateUserRequest() {
    }

    public CreateUserRequest(String username, String name, String email, String headline, String avatarUrl, String bio, String role, String location) {
        this.username = username;
        this.name = name;
        this.email = email;
        this.headline = headline;
        this.avatarUrl = avatarUrl;
        this.bio = bio;
        this.role = role;
        this.location = location;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public static class Builder {
        private String username;
        private String name;
        private String email;
        private String headline;
        private String avatarUrl;
        private String bio;
        private String role;
        private String location;

        public Builder username(String username) { this.username = username; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder headline(String headline) { this.headline = headline; return this; }
        public Builder avatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; return this; }
        public Builder bio(String bio) { this.bio = bio; return this; }
        public Builder role(String role) { this.role = role; return this; }
        public Builder location(String location) { this.location = location; return this; }

        public CreateUserRequest build() {
            return new CreateUserRequest(username, name, email, headline, avatarUrl, bio, role, location);
        }
    }
}
