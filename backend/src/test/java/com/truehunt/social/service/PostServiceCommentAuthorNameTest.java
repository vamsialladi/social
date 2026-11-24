package com.truehunt.social.service;

import com.truehunt.social.model.PostEntity;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.repository.NotificationRepository;
import com.truehunt.social.repository.OpportunityApplicationRepository;
import com.truehunt.social.repository.PostRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PostServiceCommentAuthorNameTest {

    @Test
    @SuppressWarnings("unchecked")
    void commentAndReplyResponsesUseCurrentProfileNames() {
        PostRepository postRepository = mock(PostRepository.class);
        ProfileService profileService = mock(ProfileService.class);
        PostEntity post = new PostEntity();
        post.setId(42L);
        post.addComment("Old commenter name", "", "commenter@example.com", "A comment");
        String commentId = String.valueOf(post.getComments().get(0).get("id"));
        post.addReply(commentId, "Old replier name", "", "replier@example.com", "A reply");

        ProfileEntity commenter = new ProfileEntity();
        commenter.setName("Current commenter name");
        ProfileEntity replier = new ProfileEntity();
        replier.setName("Current replier name");
        ProfileEntity newCommenter = new ProfileEntity();
        newCommenter.setName("New commenter");

        when(postRepository.findById(42L)).thenReturn(Optional.of(post));
        when(postRepository.save(any(PostEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(profileService.findCurrentProfile("commenter@example.com")).thenReturn(commenter);
        when(profileService.findCurrentProfile("replier@example.com")).thenReturn(replier);
        when(profileService.findCurrentProfile("new-commenter@example.com")).thenReturn(newCommenter);

        PostService postService = new PostService(
                postRepository,
                mock(OpportunityApplicationRepository.class),
                mock(NotificationRepository.class),
                profileService);

        Map<String, Object> response = postService.commentOnPost("42", "Another comment", "new-commenter@example.com");
        List<Map<String, Object>> comments = (List<Map<String, Object>>) response.get("comments");

        assertEquals("Current commenter name", comments.get(0).get("authorName"));
        List<Map<String, Object>> replies = (List<Map<String, Object>>) comments.get(0).get("replies");
        assertEquals("Current replier name", replies.get(0).get("authorName"));
        assertEquals("New commenter", comments.get(1).get("authorName"));
        org.junit.jupiter.api.Assertions.assertFalse(comments.get(0).containsKey("authorId"));
    }
}
