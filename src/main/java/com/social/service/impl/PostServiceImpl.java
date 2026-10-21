package com.social.service.impl;

import com.social.dto.request.PostCreateRequest;
import com.social.dto.request.PostReportRequest;
import com.social.dto.request.PostUpdateRequest;
import com.social.dto.response.CursorPageResponse;
import com.social.dto.response.MediaFileReferenceDto;
import com.social.dto.response.PostResponse;
import com.social.entity.*;
import com.social.exception.BadRequestException;
import com.social.exception.ForbiddenException;
import com.social.exception.ResourceNotFoundException;
import com.social.repository.PostReportRepository;
import com.social.repository.PostRepository;
import com.social.service.PostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class PostServiceImpl implements PostService {

    private static final Logger log = LoggerFactory.getLogger(PostServiceImpl.class);

    private final PostRepository postRepository;
    private final PostReportRepository postReportRepository;

    private static final int DEFAULT_PAGE_LIMIT = 10;
    private static final int MAX_PAGE_LIMIT = 50;
    private static final int SPAM_REPORT_THRESHOLD = 3;

    private static final Set<String> SPAM_TRIGGERS = Set.of(
            "buy crypto now", "free money fast", "click here for riches", "casino win free"
    );

    public PostServiceImpl(PostRepository postRepository, PostReportRepository postReportRepository) {
        this.postRepository = postRepository;
        this.postReportRepository = postReportRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<PostResponse> getFeed(String cursor, int limit, String viewerUserId) {
        int effectiveLimit = (limit <= 0) ? DEFAULT_PAGE_LIMIT : Math.min(limit, MAX_PAGE_LIMIT);

        Instant cursorTime = parseCursor(cursor);

        // Fetch effectiveLimit + 1 items to determine if hasMore
        List<Post> posts = postRepository.findFeedPosts(
                PostType.OPPORTUNITY,
                viewerUserId,
                cursorTime,
                PageRequest.of(0, effectiveLimit + 1)
        );

        boolean hasMore = posts.size() > effectiveLimit;
        List<Post> pagedPosts = hasMore ? posts.subList(0, effectiveLimit) : posts;

        String nextCursor = null;
        if (hasMore && !pagedPosts.isEmpty()) {
            Post lastPost = pagedPosts.get(pagedPosts.size() - 1);
            nextCursor = lastPost.getCreatedAt().toString();
        }

        List<PostResponse> responseList = pagedPosts.stream()
                .map(PostResponse::fromEntity)
                .toList();

        return CursorPageResponse.<PostResponse>builder()
                .data(responseList)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .limit(effectiveLimit)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponse getPostById(String postId, String viewerUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with ID: " + postId));

        if (post.isSpam()) {
            throw new ResourceNotFoundException("This post has been flagged and is unavailable.");
        }

        if (post.getVisibility() == PostVisibility.PRIVATE) {
            boolean isAuthor = viewerUserId != null && viewerUserId.equalsIgnoreCase(post.getAuthorId());
            if (!isAuthor) {
                throw new ForbiddenException("You do not have permission to view this private post.");
            }
        }

        return PostResponse.fromEntity(post);
    }

    @Override
    @Transactional
    public PostResponse createPost(PostCreateRequest request, String authorId) {
        if (!StringUtils.hasText(authorId)) {
            throw new BadRequestException("Author ID is required to create a post");
        }

        if (request.getType() == null) {
            throw new BadRequestException("Post type is required");
        }

        // Article validation: title is required
        if (request.getType() == PostType.ARTICLE && !StringUtils.hasText(request.getTitle())) {
            throw new BadRequestException("Title is required for articles");
        }

        // Post & Article validation: non-empty content
        if (!StringUtils.hasText(request.getContent())) {
            throw new BadRequestException("Content body cannot be empty");
        }

        // Spam filter check
        boolean flaggedAsSpam = checkSpam(request.getTitle(), request.getContent());

        List<MediaFileReference> attachments = new ArrayList<>();
        if (request.getAttachments() != null) {
            attachments = request.getAttachments().stream()
                    .filter(Objects::nonNull)
                    .map(MediaFileReferenceDto::toEntity)
                    .toList();
        }

        PostVisibility visibility = request.getVisibility() != null ? request.getVisibility() : PostVisibility.PUBLIC;

        Post post = Post.builder()
                .authorId(authorId)
                .type(request.getType())
                .title(StringUtils.hasText(request.getTitle()) ? request.getTitle().trim() : null)
                .content(request.getContent().trim())
                .attachments(new ArrayList<>(attachments))
                .visibility(visibility)
                .isSpam(flaggedAsSpam)
                .reportCount(0)
                .build();

        Post saved = postRepository.save(post);
        log.info("Post created: id={}, authorId={}, type={}, isSpam={}", saved.getId(), authorId, saved.getType(), flaggedAsSpam);

        return PostResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public PostResponse updatePost(String postId, PostUpdateRequest request, String currentUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with ID: " + postId));

        // Ownership check
        validateAuthorOwnership(post, currentUserId, "edit");

        if (post.getType() == PostType.ARTICLE && request.getTitle() != null && !StringUtils.hasText(request.getTitle())) {
            throw new BadRequestException("Article title cannot be updated to empty");
        }

        if (request.getTitle() != null) {
            post.setTitle(request.getTitle().trim());
        }

        if (request.getContent() != null) {
            if (!StringUtils.hasText(request.getContent())) {
                throw new BadRequestException("Content body cannot be empty");
            }
            post.setContent(request.getContent().trim());
            post.setSpam(checkSpam(post.getTitle(), post.getContent()));
        }

        if (request.getAttachments() != null) {
            List<MediaFileReference> attachments = request.getAttachments().stream()
                    .filter(Objects::nonNull)
                    .map(MediaFileReferenceDto::toEntity)
                    .toList();
            post.setAttachments(new ArrayList<>(attachments));
        }

        if (request.getVisibility() != null) {
            post.setVisibility(request.getVisibility());
        }

        Post saved = postRepository.save(post);
        log.info("Post updated: id={}, authorId={}", saved.getId(), currentUserId);

        return PostResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void deletePost(String postId, String currentUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with ID: " + postId));

        // Ownership check
        validateAuthorOwnership(post, currentUserId, "delete");

        postRepository.delete(post);
        log.info("Post deleted: id={}, authorId={}", postId, currentUserId);
    }

    @Override
    @Transactional
    public void reportPost(String postId, PostReportRequest request, String reporterUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with ID: " + postId));

        if (postReportRepository.existsByPostIdAndReporterUserId(postId, reporterUserId)) {
            throw new BadRequestException("You have already reported this post.");
        }

        PostReport report = PostReport.builder()
                .postId(postId)
                .reporterUserId(reporterUserId)
                .reason(request != null ? request.getReason() : "Reported as inappropriate/spam")
                .build();
        postReportRepository.save(report);

        int newReportCount = post.getReportCount() + 1;
        post.setReportCount(newReportCount);

        if (newReportCount >= SPAM_REPORT_THRESHOLD) {
            post.setSpam(true);
            log.warn("Post {} flagged as spam due to report threshold ({})", postId, newReportCount);
        }

        postRepository.save(post);
    }

    private void validateAuthorOwnership(Post post, String currentUserId, String action) {
        if (!StringUtils.hasText(currentUserId) || !currentUserId.equalsIgnoreCase(post.getAuthorId())) {
            throw new ForbiddenException("Only the author can " + action + " this post.");
        }
    }

    private boolean checkSpam(String title, String content) {
        String combined = ((title != null ? title : "") + " " + (content != null ? content : "")).toLowerCase();
        return SPAM_TRIGGERS.stream().anyMatch(combined::contains);
    }

    private Instant parseCursor(String cursor) {
        if (!StringUtils.hasText(cursor)) {
            return null;
        }
        try {
            return Instant.parse(cursor.trim());
        } catch (DateTimeParseException e) {
            try {
                long epochMillis = Long.parseLong(cursor.trim());
                return Instant.ofEpochMilli(epochMillis);
            } catch (NumberFormatException nfe) {
                log.warn("Could not parse cursor '{}', treating as null", cursor);
                return null;
            }
        }
    }
}
