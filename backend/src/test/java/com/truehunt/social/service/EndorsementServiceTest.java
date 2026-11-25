package com.truehunt.social.service;

import com.truehunt.social.exception.ConflictException;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.repository.EndorsementRepository;
import com.truehunt.social.repository.ProfileRepository;
import com.truehunt.social.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({EndorsementService.class, JacksonAutoConfiguration.class})
class EndorsementServiceTest {

    @Autowired
    private EndorsementService endorsementService;

    @Autowired
    private EndorsementRepository endorsementRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Test
    void savesEndorsementForTargetAndReturnsItToOtherProfileVisitors() {
        saveProfile("OWNER01", "owner@example.test", "Profile owner");
        saveProfile("WRITER01", "writer@example.test", "Profile writer");

        Map<String, Object> created = endorsementService.createEndorsement(
                "OWNER01",
                Map.of(
                        "endorsementMessage", "A reliable collaborator.",
                        "visibility", "Public",
                        "endorserName", "Untrusted display name"
                ),
                "writer@example.test"
        );

        List<Map<String, Object>> visibleEndorsements =
                endorsementService.getProfileEndorsements("OWNER01", null);

        assertEquals(1, visibleEndorsements.size());
        assertEquals(created.get("endorsementId"), visibleEndorsements.get(0).get("endorsementId"));
        assertEquals("Profile writer", visibleEndorsements.get(0).get("endorserName"));
        assertEquals("OWNER01", visibleEndorsements.get(0).get("receiverProfileId"));
        assertEquals(1, endorsementRepository.count());
    }

    @Test
    void privateEndorsementIsVisibleOnlyToItsOwnerAndAuthor() {
        saveProfile("OWNER01", "owner@example.test", "Profile owner");
        saveProfile("WRITER01", "writer@example.test", "Profile writer");
        endorsementService.createEndorsement(
                "OWNER01",
                Map.of("endorsementMessage", "For the owner only.", "visibility", "Private"),
                "writer@example.test"
        );

        assertEquals(0, endorsementService.getProfileEndorsements("OWNER01", null).size());
        assertEquals(1, endorsementService.getProfileEndorsements("OWNER01", "owner@example.test").size());
        assertEquals(1, endorsementService.getProfileEndorsements("OWNER01", "writer@example.test").size());
    }

    @Test
    void preventsTheSamePersonFromEndorsingTheSameProfileTwice() {
        saveProfile("OWNER01", "owner@example.test", "Profile owner");
        saveProfile("WRITER01", "writer@example.test", "Profile writer");
        Map<String, Object> request = Map.of("endorsementMessage", "A reliable collaborator.");
        endorsementService.createEndorsement("OWNER01", request, "writer@example.test");

        assertThrows(
                ConflictException.class,
                () -> endorsementService.createEndorsement("OWNER01", request, "writer@example.test")
        );
    }

    private void saveProfile(String userId, String email, String name) {
        UserAccountEntity account = new UserAccountEntity();
        account.setEmail(email);
        account.setPasswordHash("test-hash");
        account.setPublicUserId(userId);
        account.setName(name);
        account.setRole("TALENT");
        userAccountRepository.saveAndFlush(account);

        ProfileEntity profile = new ProfileEntity();
        profile.setUserId(userId);
        profile.setEmail(email);
        profile.setName(name);
        profile.setPrimaryCraft("Film");
        profileRepository.saveAndFlush(profile);
    }
}
