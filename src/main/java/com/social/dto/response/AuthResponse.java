package com.social.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private UserResponse user;
    private ProfileResponse profile;

    public AuthResponse() {
    }

    public AuthResponse(UserResponse user, ProfileResponse profile) {
        this.user = user;
        this.profile = profile;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UserResponse user;
        private ProfileResponse profile;

        public Builder user(UserResponse user) {
            this.user = user;
            return this;
        }

        public Builder profile(ProfileResponse profile) {
            this.profile = profile;
            return this;
        }

        public AuthResponse build() {
            return new AuthResponse(user, profile);
        }
    }

    public UserResponse getUser() {
        return user;
    }

    public void setUser(UserResponse user) {
        this.user = user;
    }

    public ProfileResponse getProfile() {
        return profile;
    }

    public void setProfile(ProfileResponse profile) {
        this.profile = profile;
    }
}
