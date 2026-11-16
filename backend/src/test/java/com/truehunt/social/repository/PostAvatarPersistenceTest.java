package com.truehunt.social.repository;

import com.truehunt.social.model.PostEntity;
import com.truehunt.social.model.OpportunityApplicationEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class PostAvatarPersistenceTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private OpportunityApplicationRepository opportunityApplicationRepository;

    @Test
    void persistsLargeProfileAvatarOnOpportunityPost() {
        String avatar = "data:image/jpeg;base64," + "A".repeat(60_000);
        PostEntity post = new PostEntity();
        post.setType("opportunity");
        post.setAuthorAvatar(avatar);

        PostEntity saved = postRepository.saveAndFlush(post);
        String savedAvatar = postRepository.findById(saved.getId())
                .orElseThrow()
                .getAuthorAvatar();

        assertEquals(avatar, savedAvatar);
    }

    @Test
    void storesApplicationsSeparatelyForEachApplicant() {
        opportunityApplicationRepository.save(new OpportunityApplicationEntity(42L, "first@example.com"));
        opportunityApplicationRepository.saveAndFlush(
                new OpportunityApplicationEntity(42L, "second@example.com")
        );

        assertEquals(2, opportunityApplicationRepository.countByPostId(42L));
        assertEquals("first@example.com", opportunityApplicationRepository
                .findByPostIdAndApplicantIdentity(42L, "first@example.com")
                .orElseThrow()
                .getApplicantIdentity());
        assertEquals("second@example.com", opportunityApplicationRepository
                .findByPostIdAndApplicantIdentity(42L, "second@example.com")
                .orElseThrow()
                .getApplicantIdentity());
    }
}
