package com.truehunt.social.service;

import com.truehunt.social.dto.ProfileDto;
import com.truehunt.social.exception.ResourceNotFoundException;
import com.truehunt.social.model.PostEntity;
import com.truehunt.social.repository.PostRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PostServicePostLikesTest {

    @Test
    void returnsPublicProfileSummariesForPeopleWhoLikedAVisiblePost() {
        PostRepository posts = mock(PostRepository.class);
        ProfileService profiles = mock(ProfileService.class);
        PostEntity post = new PostEntity();
        post.setId(42L);
        post.setAudience("public");
        post.toggleLikeForUser("liker@example.com");
        when(posts.findById(42L)).thenReturn(Optional.of(post));

        ProfileDto liker = new ProfileDto();
        liker.setId("liker-id");
        liker.setUserId("liker-id");
        liker.setName("Asha Rao");
        liker.setProfileImageUrl("/uploads/asha.jpg");
        when(profiles.getProfileById("liker@example.com", "viewer@example.com")).thenReturn(liker);

        PostService service = new PostService(posts, null, null, profiles);

        List<Map<String, Object>> likers = service.getPostLikes("42", "viewer@example.com");

        assertEquals(1, likers.size());
        Map<String, Object> likerSummary = likers.get(0);
        assertEquals("Asha Rao", likerSummary.get("name"));
        assertEquals("/uploads/asha.jpg", likerSummary.get("profileImageUrl"));
        assertFalse(likerSummary.containsKey("email"));
    }

    @Test
    void doesNotReturnLikesForAPrivatePostTheViewerCannotSee() {
        PostRepository posts = mock(PostRepository.class);
        ProfileService profiles = mock(ProfileService.class);
        PostEntity post = new PostEntity();
        post.setId(42L);
        post.setAudience("private");
        post.toggleLikeForUser("liker@example.com");
        when(posts.findById(42L)).thenReturn(Optional.of(post));

        PostService service = new PostService(posts, null, null, profiles);

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.getPostLikes("42", "viewer@example.com")
        );
    }
}
