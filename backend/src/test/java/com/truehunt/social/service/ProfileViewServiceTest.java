package com.truehunt.social.service;

import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.repository.ProfileRepository;
import com.truehunt.social.repository.ProfileViewRepository;
import com.truehunt.social.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(ProfileViewService.class)
class ProfileViewServiceTest {

    @Autowired
    private ProfileViewService profileViewService;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private ProfileViewRepository profileViewRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Test
    void recordsPublicIdViewerAndReturnsTheirProfileDetails() {
        UserAccountEntity owner = saveAccount("owner@example.com", "OWNER01", "Profile owner");
        UserAccountEntity viewer = saveAccount("viewer@example.com", "VIEWER1", "Profile viewer");
        saveProfile(owner, "https://example.test/owner.jpg");
        saveProfile(viewer, "https://example.test/viewer.jpg");

        assertEquals(Map.of("recorded", true),
                profileViewService.recordView("OWNER01", "VIEWER1"));
        profileViewService.recordView("OWNER01", "viewer@example.com");

        List<Map<String, Object>> viewers = profileViewService.getMyViewers("OWNER01");

        assertEquals(1, viewers.size());
        assertEquals("Profile viewer", viewers.get(0).get("name"));
        assertEquals("https://example.test/viewer.jpg", viewers.get(0).get("profileImageUrl"));
        assertEquals("VIEWER1", viewers.get(0).get("userId"));
        assertEquals(1, profileViewRepository.count());
    }

    private UserAccountEntity saveAccount(String email, String publicUserId, String name) {
        UserAccountEntity account = new UserAccountEntity();
        account.setEmail(email);
        account.setPasswordHash("hash");
        account.setPublicUserId(publicUserId);
        account.setName(name);
        account.setRole("TALENT");
        return userAccountRepository.saveAndFlush(account);
    }

    private void saveProfile(UserAccountEntity account, String imageUrl) {
        ProfileEntity profile = new ProfileEntity();
        profile.setUserId(account.getPublicUserId());
        profile.setEmail(account.getEmail());
        profile.setName(account.getName());
        profile.setProfileImageUrl(imageUrl);
        profileRepository.saveAndFlush(profile);
    }
}