package com.truehunt.social.service;

import com.truehunt.social.exception.UnauthorizedException;
import com.truehunt.social.model.PostEntity;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.repository.NotificationRepository;
import com.truehunt.social.repository.OpportunityApplicationRepository;
import com.truehunt.social.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostServiceCommentDeletionTest {

    private PostRepository postRepository;
    private ProfileService profileService;
    private PostService postService;
    private PostEntity post;
    private ProfileEntity ownerProfile;
    private ProfileEntity commenterProfile;
    private String commentId;

    @BeforeEach
    void setUp() {
        postRepository = mock(PostRepository.class);
        profileService = mock(ProfileService.class);
        postService = new PostService(
                postRepository,
                mock(OpportunityApplicationRepository.class),
                mock(NotificationRepository.class),
                profileService
        );

        ownerProfile = new ProfileEntity();
        ownerProfile.setUserId("owner-1");
        ownerProfile.setEmail("owner@example.com");
        commenterProfile = new ProfileEntity();
        commenterProfile.setUserId("commenter-1");
        commenterProfile.setEmail("commenter@example.com");

        post = new PostEntity();
        post.setId(42L);
        post.setAuthorId(ownerProfile.getUserId());
        post.addComment("Commenter", "", "commenter@example.com", "A comment");
        commentId = String.valueOf(post.getComments().get(0).get("id"));

        when(postRepository.findById(42L)).thenReturn(Optional.of(post));
        when(postRepository.save(any(PostEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(profileService.findCurrentProfile("owner@example.com")).thenReturn(ownerProfile);
        when(profileService.findCurrentProfile("commenter@example.com")).thenReturn(commenterProfile);
    }

    @Test
    void commentAuthorCanDeleteTheirComment() {
        postService.deleteComment("42", commentId, "commenter@example.com");

        assertEquals(0, post.getCommentsCount());
        verify(postRepository).save(post);
    }

    @Test
    void postOwnerCanDeleteAnotherUsersComment() {
        postService.deleteComment("42", commentId, "owner@example.com");

        assertEquals(0, post.getCommentsCount());
        verify(postRepository).save(post);
    }

    @Test
    void unrelatedUserCannotDeleteComment() {
        ProfileEntity otherProfile = new ProfileEntity();
        otherProfile.setUserId("other-1");
        otherProfile.setEmail("other@example.com");
        when(profileService.findCurrentProfile("other@example.com")).thenReturn(otherProfile);

        assertThrows(UnauthorizedException.class,
                () -> postService.deleteComment("42", commentId, "other@example.com"));

        assertEquals(1, post.getCommentsCount());
        verify(postRepository, never()).save(any(PostEntity.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void replyAuthorCanDeleteTheirReply() {
        post.addReply(commentId, "Reply author", "", "commenter@example.com", "A reply");
        List<Map<String, Object>> replies = (List<Map<String, Object>>) post.getComments().get(0).get("replies");
        String replyId = String.valueOf(replies.get(0).get("id"));

        postService.deleteCommentReply("42", commentId, replyId, "commenter@example.com");

        List<Map<String, Object>> remainingReplies =
                (List<Map<String, Object>>) post.getComments().get(0).get("replies");
        assertEquals(0, remainingReplies.size());
        verify(postRepository).save(post);
    }
}
