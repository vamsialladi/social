package com.social.service;

import com.social.dto.request.PostCreateRequest;
import com.social.dto.request.PostReportRequest;
import com.social.dto.request.PostUpdateRequest;
import com.social.dto.response.CursorPageResponse;
import com.social.dto.response.PostResponse;
import com.social.entity.Post;
import com.social.entity.PostReport;
import com.social.entity.PostType;
import com.social.entity.PostVisibility;
import com.social.exception.BadRequestException;
import com.social.exception.ForbiddenException;
import com.social.exception.ResourceNotFoundException;
import com.social.repository.PostReportRepository;
import com.social.repository.PostRepository;
import com.social.service.impl.PostServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostReportRepository postReportRepository;

    @InjectMocks
    private PostServiceImpl postService;

    @Test
    void createPost_regularPost_succeeds() {
        PostCreateRequest request = PostCreateRequest.builder()
                .type(PostType.POST)
                .content("Hello world, this is my first post!")
                .visibility(PostVisibility.PUBLIC)
                .build();

        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PostResponse response = postService.createPost(request, "user-100");

        assertNotNull(response);
        assertEquals("user-100", response.getAuthorId());
        assertEquals(PostType.POST, response.getType());
        assertEquals("Hello world, this is my first post!", response.getContent());
    }

    @Test
    void createPost_articleWithoutTitle_throwsBadRequestException() {
        PostCreateRequest request = PostCreateRequest.builder()
                .type(PostType.ARTICLE)
                .title("   ")
                .content("Article body without title")
                .build();

        assertThrows(BadRequestException.class, () -> 
                postService.createPost(request, "user-100"));
    }

    @Test
    void createPost_articleWithTitle_succeeds() {
        PostCreateRequest request = PostCreateRequest.builder()
                .type(PostType.ARTICLE)
                .title("The Future of Digital Arts")
                .content("Article body content goes here in depth...")
                .build();

        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PostResponse response = postService.createPost(request, "user-100");

        assertNotNull(response);
        assertEquals("The Future of Digital Arts", response.getTitle());
        assertEquals(PostType.ARTICLE, response.getType());
    }

    @Test
    void createPost_emptyContent_throwsBadRequestException() {
        PostCreateRequest request = PostCreateRequest.builder()
                .type(PostType.POST)
                .content("   ")
                .build();

        assertThrows(BadRequestException.class, () -> 
                postService.createPost(request, "user-100"));
    }

    @Test
    void getFeed_cursorPagination_returnsPostsAndNextCursor() {
        Instant now = Instant.now();
        Post post1 = Post.builder()
                .id("post-1")
                .authorId("user-1")
                .type(PostType.POST)
                .content("First post")
                .createdAt(now)
                .build();

        Post post2 = Post.builder()
                .id("post-2")
                .authorId("user-2")
                .type(PostType.ARTICLE)
                .title("Article 2")
                .content("Second post")
                .createdAt(now.minusSeconds(60))
                .build();

        when(postRepository.findFeedPosts(eq(PostType.OPPORTUNITY), eq("viewer-1"), any(), any(Pageable.class)))
                .thenReturn(List.of(post1, post2));

        CursorPageResponse<PostResponse> feed = postService.getFeed(null, 10, "viewer-1");

        assertNotNull(feed);
        assertEquals(2, feed.getData().size());
        assertFalse(feed.isHasMore());
    }

    @Test
    void updatePost_byAuthor_succeeds() {
        Post existing = Post.builder()
                .id("post-1")
                .authorId("author-100")
                .type(PostType.POST)
                .content("Original content")
                .build();

        when(postRepository.findById("post-1")).thenReturn(Optional.of(existing));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PostUpdateRequest updateRequest = PostUpdateRequest.builder()
                .content("Updated content")
                .build();

        PostResponse response = postService.updatePost("post-1", updateRequest, "author-100");

        assertNotNull(response);
        assertEquals("Updated content", response.getContent());
    }

    @Test
    void updatePost_byDifferentUser_throwsForbiddenException() {
        Post existing = Post.builder()
                .id("post-1")
                .authorId("author-100")
                .type(PostType.POST)
                .content("Original content")
                .build();

        when(postRepository.findById("post-1")).thenReturn(Optional.of(existing));

        PostUpdateRequest updateRequest = PostUpdateRequest.builder()
                .content("Hacked content")
                .build();

        assertThrows(ForbiddenException.class, () -> 
                postService.updatePost("post-1", updateRequest, "malicious-user"));
    }

    @Test
    void deletePost_byDifferentUser_throwsForbiddenException() {
        Post existing = Post.builder()
                .id("post-1")
                .authorId("author-100")
                .build();

        when(postRepository.findById("post-1")).thenReturn(Optional.of(existing));

        assertThrows(ForbiddenException.class, () -> 
                postService.deletePost("post-1", "other-user"));
    }

    @Test
    void reportPost_thirdReport_triggersSpamFlag() {
        Post post = Post.builder()
                .id("post-1")
                .authorId("author-100")
                .reportCount(2)
                .isSpam(false)
                .build();

        when(postRepository.findById("post-1")).thenReturn(Optional.of(post));
        when(postReportRepository.existsByPostIdAndReporterUserId("post-1", "user-3")).thenReturn(false);
        when(postReportRepository.save(any(PostReport.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PostReportRequest reportRequest = PostReportRequest.builder()
                .reason("Spam link")
                .build();

        postService.reportPost("post-1", reportRequest, "user-3");

        assertEquals(3, post.getReportCount());
        assertTrue(post.isSpam(), "Post should be flagged as spam upon reaching 3 reports");
    }
}
