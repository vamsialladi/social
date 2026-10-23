package com.truehunt.dto.response;

public class AuthResponse {
    private String token;
    private UserResponse user;
    private ProfileResponse profile;

    public AuthResponse() {}

    public AuthResponse(String token, UserResponse user, ProfileResponse profile) {
        this.token = token;
        this.user = user;
        this.profile = profile;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
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
