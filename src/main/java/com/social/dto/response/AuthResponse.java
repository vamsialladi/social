package com.social.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private String token;
    private String tokenType;
    private UserResponse user;
    private ProfileResponse profile;

    public AuthResponse() {
    }

    public AuthResponse(String token, String tokenType, UserResponse user, ProfileResponse profile) {
        this.token = token;
        this.tokenType = tokenType;
        this.user = user;
        this.profile = profile;
    }

    public AuthResponse(UserResponse user, ProfileResponse profile) {
        this(null, null, user, profile);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String token;
        private String tokenType = "Bearer";
        private UserResponse user;
        private ProfileResponse profile;

        public Builder token(String token) {
            this.token = token;
            return this;
        }

        public Builder tokenType(String tokenType) {
            this.tokenType = tokenType;
            return this;
        }

        public Builder user(UserResponse user) {
            this.user = user;
            return this;
        }

        public Builder profile(ProfileResponse profile) {
            this.profile = profile;
            return this;
        }

        public AuthResponse build() {
            return new AuthResponse(token, tokenType, user, profile);
        }
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
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
