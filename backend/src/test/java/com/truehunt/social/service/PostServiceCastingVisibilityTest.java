package com.truehunt.social.service;

import com.truehunt.social.dto.CreatePostRequest;
import com.truehunt.social.model.NotificationEntity;
import com.truehunt.social.model.PostEntity;
import com.truehunt.social.repository.NotificationRepository;
import com.truehunt.social.repository.OpportunityApplicationRepository;
import com.truehunt.social.repository.PostRepository;
import com.truehunt.social.model.ProfileEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostServiceCastingVisibilityTest {

    private PostRepository postRepository;
    private NotificationRepository notificationRepository;
    private ProfileService profileService;
    private PostService postService;

    @BeforeEach
    void setUp() {
        postRepository = mock(PostRepository.class);
        notificationRepository = mock(NotificationRepository.class);
        profileService = mock(ProfileService.class);
        postService = new PostService(
                postRepository,
                mock(OpportunityApplicationRepository.class),
                notificationRepository,
                profileService
        );
    }

    @Test
    void castingPostsAppearOnlyInCastingQueries() {
        PostEntity castingPost = post("casting");
        PostEntity regularPost = post("post");
        when(postRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(castingPost, regularPost));
        when(postRepository.findAllByTypeIgnoreCaseOrderByCreatedAtDesc("casting"))
                .thenReturn(List.of(castingPost));
        when(postRepository.findAllByAuthorIdOrderByCreatedAtDesc("author-1"))
                .thenReturn(List.of(castingPost, regularPost));

        List<Map<String, Object>> feed = postService.getPosts("", "viewer@example.com");
        List<Map<String, Object>> castingPage = postService.getPosts("casting", "viewer@example.com");
        List<Map<String, Object>> profilePosts =
                postService.getPostsByAuthorId("author-1", "viewer@example.com");

        assertEquals(List.of("post"), feed.stream().map(post -> post.get("type")).toList());
        assertEquals(List.of("casting"), castingPage.stream().map(post -> post.get("type")).toList());
        assertEquals(List.of("post"), profilePosts.stream().map(post -> post.get("type")).toList());
    }

    @Test
    void creatingCastingProfileDoesNotCreateSocialPostNotifications() {
        ProfileEntity owner = new ProfileEntity();
        owner.setUserId("owner-1");
        owner.setEmail("owner@example.com");
        when(profileService.findCurrentProfile("owner@example.com")).thenReturn(owner);
        when(postRepository.save(any(PostEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        CreatePostRequest request = new CreatePostRequest();
        request.setType("casting");
        request.setTitle("Casting profile");
        request.setContent("Acting experience");

        postService.createPost(request, "owner@example.com");

        verify(notificationRepository, never()).save(any(NotificationEntity.class));
    }

    private PostEntity post(String type) {
        PostEntity post = new PostEntity();
        post.setId((long) (type.equals("casting") ? 1 : 2));
        post.setType(type);
        post.setAuthorId("author-1");
        post.setAudience("public");
        return post;
    }
}
