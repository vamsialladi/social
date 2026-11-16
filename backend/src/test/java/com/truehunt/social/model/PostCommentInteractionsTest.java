package com.truehunt.social.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostCommentInteractionsTest {

    @Test
    void commentLikesTogglePerUserAndDoNotExposeUserIdentifiers() {
        PostEntity post = new PostEntity();
        post.addComment("Comment author", "A comment");
        String commentId = String.valueOf(post.getComments().get(0).get("id"));

        assertTrue(post.toggleCommentLike(commentId, "first@example.com"));

        Map<String, Object> firstUserComment = getFirstComment(post.toDto("first@example.com"));
        Map<String, Object> otherUserComment = getFirstComment(post.toDto("other@example.com"));
        assertEquals(1, firstUserComment.get("likesCount"));
        assertEquals(true, firstUserComment.get("likedByMe"));
        assertEquals(false, otherUserComment.get("likedByMe"));
        assertFalse(firstUserComment.containsKey("likedByUsers"));

        assertTrue(post.toggleCommentLike(commentId, "FIRST@example.com"));
        assertEquals(0, getFirstComment(post.toDto("first@example.com")).get("likesCount"));
    }

    @Test
    void repliesAreStoredUnderTheirParentComment() {
        PostEntity post = new PostEntity();
        post.addComment("Comment author", "A comment");
        String commentId = String.valueOf(post.getComments().get(0).get("id"));

        assertTrue(post.addReply(commentId, "Reply author", "https://example.com/reply.png", "A reply"));

        Map<String, Object> comment = post.getComments().get(0);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> replies = (List<Map<String, Object>>) comment.get("replies");
        assertEquals(1, replies.size());
        assertEquals("Reply author", replies.get(0).get("authorName"));
        assertEquals("https://example.com/reply.png", replies.get(0).get("authorAvatar"));
        assertEquals("A reply", replies.get(0).get("text"));
        assertFalse(post.addReply("missing-comment", "Reply author", "Another reply"));
    }

    @Test
    void commentsStoreTheirAuthorsProfileImage() {
        PostEntity post = new PostEntity();
        post.addComment("Comment author", "https://example.com/comment.png", "A comment");

        assertEquals("https://example.com/comment.png", post.getComments().get(0).get("authorAvatar"));
    }

    @Test
    void commentsAndRepliesExposeDeletePermissionWithoutAuthorIdentifiers() {
        PostEntity post = new PostEntity();
        post.setAuthorId("post-owner@example.com");
        post.addComment("Comment author", "", "author@example.com", "A comment");
        String commentId = String.valueOf(post.getComments().get(0).get("id"));
        post.addReply(commentId, "Reply author", "", "reply@example.com", "A reply");

        Map<String, Object> comment = getFirstComment(post.toDto("author@example.com"));
        assertEquals(true, comment.get("canDelete"));
        assertFalse(comment.containsKey("authorId"));
        assertEquals(false, getFirstComment(post.toDto("post-owner@example.com")).get("canDelete"));
        assertEquals(false, getFirstComment(post.toDto("other@example.com")).get("canDelete"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> replies = (List<Map<String, Object>>) comment.get("replies");
        assertEquals(false, replies.get(0).get("canDelete"));
        assertFalse(replies.get(0).containsKey("authorId"));

        Map<String, Object> replyAuthorView = getFirstComment(post.toDto("reply@example.com"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> replyAuthorReplies = (List<Map<String, Object>>) replyAuthorView.get("replies");
        assertEquals(true, replyAuthorReplies.get(0).get("canDelete"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void deletingCommentsAndRepliesUpdatesStoredComments() {
        PostEntity post = new PostEntity();
        post.addComment("Comment author", "", "author@example.com", "A comment");
        String commentId = String.valueOf(post.getComments().get(0).get("id"));
        post.addReply(commentId, "Reply author", "", "reply@example.com", "A reply");
        String replyId = String.valueOf(((List<Map<String, Object>>) post.getComments().get(0).get("replies"))
                .get(0).get("id"));

        assertTrue(post.deleteReply(commentId, replyId));
        assertTrue(((List<Map<String, Object>>) post.getComments().get(0).get("replies")).isEmpty());
        assertTrue(post.deleteComment(commentId));
        assertEquals(0, post.getCommentsCount());
        assertFalse(post.deleteComment(commentId));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getFirstComment(Map<String, Object> postDto) {
        return ((List<Map<String, Object>>) postDto.get("comments")).get(0);
    }
}
