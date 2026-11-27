package com.truehunt.social.dto;

import java.util.List;

public class CreateConnectionGroupRequest {

    private String name;
    private String imageUrl;
    private String publicUrl;
    private String website;
    private String industry;
    private String tagline;
    private String bannerUrl;
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

    public String getPublicUrl() {
        return publicUrl;
    }

    public void setPublicUrl(String publicUrl) {
        this.publicUrl = publicUrl;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public String getBannerUrl() {
        return bannerUrl;
    }

    public void setBannerUrl(String bannerUrl) {
        this.bannerUrl = bannerUrl;
    }

    public List<String> getMemberProfileIds() {
        return memberProfileIds;
    }

    public void setMemberProfileIds(List<String> memberProfileIds) {
        this.memberProfileIds = memberProfileIds;
    }
}
