package com.truehunt.social.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PostEntityOpportunityDetailsTest {

    @Test
    void keepsOpportunityDetailsSeparateFromMediaAndInDto() {
        PostEntity post = new PostEntity();
        post.setType("opportunity");
        post.setMedia(Map.of("url", "/uploads/casting.jpg", "kind", "image"));
        Map<String, Object> details = Map.of(
                "projectName", "Upcoming Film",
                "languagesKnown", List.of("Telugu", "English"),
                "guardianPermission", false
        );

        post.setOpportunityDetails(details);

        assertEquals(details, post.toDto().get("opportunityDetails"));
        assertEquals("/uploads/casting.jpg", post.getMedia().get("url"));
    }

    @Test
    void readsPreviouslySavedOpportunityDetails() {
        PostEntity post = new PostEntity();
        post.setType("opportunity");
        Map<String, Object> details = Map.of(
                "projectName", "Existing Film",
                "actingRole", "Hero / Lead"
        );
        post.setMedia(details);

        assertEquals(details, post.getOpportunityDetails());
    }
}
