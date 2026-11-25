package com.truehunt.social.dto;

import java.util.List;

public class CreateConnectionGroupRequest {

    private String name;
    private String imageUrl;
    private List<String> memberProfileIds;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public List<String> getMemberProfileIds() {
        return memberProfileIds;
    }

    public void setMemberProfileIds(List<String> memberProfileIds) {
        this.memberProfileIds = memberProfileIds;
    }
}
