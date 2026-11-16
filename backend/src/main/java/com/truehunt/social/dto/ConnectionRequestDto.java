package com.truehunt.social.dto;

public class ConnectionRequestDto {
    private String talentId;
    private String profileId;
    private String message;
    private String timestamp;

    public ConnectionRequestDto() {}

    public String getTalentId() {
        return talentId != null ? talentId : profileId;
    }

    public void setTalentId(String talentId) {
        this.talentId = talentId;
    }

    public String getProfileId() {
        return profileId != null ? profileId : talentId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}

