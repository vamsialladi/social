package com.social.service.impl;

import com.social.dto.request.CreateOpportunityRequest;
import com.social.dto.request.CreatePostRequest;
import com.social.dto.response.CursorPageResponse;
import com.social.dto.response.OpportunityResponse;
import com.social.entity.Post;
import com.social.entity.User;
import com.social.exception.BadRequestException;
import com.social.exception.ResourceNotFoundException;
import com.social.repository.PostRepository;
import com.social.repository.UserRepository;
import com.social.service.OpportunityService;
import com.social.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OpportunityServiceImpl implements OpportunityService {

    private static final Logger log = LoggerFactory.getLogger(OpportunityServiceImpl.class);

    public static final String TYPE_OPPORTUNITY = "opportunity";
    public static final String TYPE_POST = "post";
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public OpportunityServiceImpl(PostRepository postRepository, UserRepository userRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public OpportunityResponse createOpportunity(String authorId, CreateOpportunityRequest request) {
        if (authorId == null || authorId.trim().isEmpty()) {
            throw new BadRequestException("Author ID is required to create an opportunity");
        }

        // Validate author exists
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Author not found with ID: " + authorId));

        // Validate expiry date if provided
        if (request.getExpiryDate() != null && request.getExpiryDate().isBefore(Instant.now())) {
            throw new BadRequestException("Expiry date cannot be in the past");
        }

        Instant now = Instant.now();
        Post post = Post.builder()
                .id(IdGenerator.nextObjectId())
                .authorId(author.getId())
                .authorName(author.getName())
                .authorAvatarUrl(author.getAvatarUrl())
                .title(request.getTitle().trim())
                .content(request.getContent().trim())
                // Rule: Server forces type = opportunity
                .type(TYPE_OPPORTUNITY)
                .employer(request.getEmployer() != null ? request.getEmployer().trim() : author.getName())
                .compensation(request.getCompensation() != null ? request.getCompensation().trim() : null)
                .location(request.getLocation() != null ? request.getLocation().trim() : null)
                .expiryDate(request.getExpiryDate())
                .mediaUrls(request.getMediaUrls() != null ? new ArrayList<>(request.getMediaUrls()) : new ArrayList<>())
                .applicationsCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Post saved = postRepository.save(post);
        log.info("Created opportunity ID {} by author {}", saved.getId(), authorId);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<OpportunityResponse> getOpportunities(String cursor, int limit) {
        int sanitizedLimit = sanitizeLimit(limit);
        Pageable pageable = PageRequest.of(0, sanitizedLimit + 1);

        List<Post> posts;
        if (cursor != null && !cursor.trim().isEmpty()) {
            Instant cursorTimestamp = parseCursor(cursor.trim());
            posts = postRepository.findByTypeAndCreatedAtLessThanOrderByCreatedAtDesc(TYPE_OPPORTUNITY, cursorTimestamp, pageable);
        } else {
            posts = postRepository.findByTypeOrderByCreatedAtDesc(TYPE_OPPORTUNITY, pageable);
        }

        // If no real records exist, API returns an empty list and UI shows its empty state
        if (posts.isEmpty()) {
            return CursorPageResponse.empty();
        }

        boolean hasMore = posts.size() > sanitizedLimit;
        List<Post> resultPosts = hasMore ? posts.subList(0, sanitizedLimit) : posts;

        String nextCursor = null;
        if (hasMore && !resultPosts.isEmpty()) {
            nextCursor = resultPosts.get(resultPosts.size() - 1).getCreatedAt().toString();
        }

        List<OpportunityResponse> responseList = resultPosts.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        long totalCount = postRepository.countByType(TYPE_OPPORTUNITY);

        return CursorPageResponse.<OpportunityResponse>builder()
                .items(responseList)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .totalCount(totalCount)
                .build();
    }

    @Override
    @Transactional
    public OpportunityResponse createPost(String authorId, CreatePostRequest request) {
        if (authorId == null || authorId.trim().isEmpty()) {
            throw new BadRequestException("Author ID is required to create a post");
        }

        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Author not found with ID: " + authorId));

        Instant now = Instant.now();
        Post post = Post.builder()
                .id(IdGenerator.nextObjectId())
                .authorId(author.getId())
                .authorName(author.getName())
                .authorAvatarUrl(author.getAvatarUrl())
                .content(request.getContent().trim())
                .type(TYPE_POST)
                .mediaUrls(request.getMediaUrls() != null ? new ArrayList<>(request.getMediaUrls()) : new ArrayList<>())
                .applicationsCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Post saved = postRepository.save(post);
        log.info("Created normal post ID {} by author {}", saved.getId(), authorId);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<OpportunityResponse> getOrdinaryPostsFeed(String cursor, int limit) {
        // Rule: Opportunity posts must not appear in the ordinary posts feed
        int sanitizedLimit = sanitizeLimit(limit);
        Pageable pageable = PageRequest.of(0, sanitizedLimit + 1);

        List<Post> posts;
        if (cursor != null && !cursor.trim().isEmpty()) {
            Instant cursorTimestamp = parseCursor(cursor.trim());
            posts = postRepository.findByTypeNotAndCreatedAtLessThanOrderByCreatedAtDesc(TYPE_OPPORTUNITY, cursorTimestamp, pageable);
        } else {
            posts = postRepository.findByTypeNotOrderByCreatedAtDesc(TYPE_OPPORTUNITY, pageable);
        }

        if (posts.isEmpty()) {
            return CursorPageResponse.empty();
        }

        boolean hasMore = posts.size() > sanitizedLimit;
        List<Post> resultPosts = hasMore ? posts.subList(0, sanitizedLimit) : posts;

        String nextCursor = null;
        if (hasMore && !resultPosts.isEmpty()) {
            nextCursor = resultPosts.get(resultPosts.size() - 1).getCreatedAt().toString();
        }

        List<OpportunityResponse> responseList = resultPosts.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return CursorPageResponse.<OpportunityResponse>builder()
                .items(responseList)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .totalCount(responseList.size())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OpportunityResponse getOpportunityById(String id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found with ID: " + id));
        if (!TYPE_OPPORTUNITY.equalsIgnoreCase(post.getType())) {
            throw new ResourceNotFoundException("Record with ID " + id + " is not an opportunity");
        }
        return mapToResponse(post);
    }

    private int sanitizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private Instant parseCursor(String cursor) {
        try {
            return Instant.parse(cursor);
        } catch (DateTimeParseException ex) {
            try {
                long epochMillis = Long.parseLong(cursor);
                return Instant.ofEpochMilli(epochMillis);
            } catch (NumberFormatException nfe) {
                throw new BadRequestException("Invalid cursor format. Expected ISO-8601 timestamp string or epoch millis: " + cursor);
            }
        }
    }

    private OpportunityResponse mapToResponse(Post post) {
        return OpportunityResponse.builder()
                .id(post.getId())
                .mongoId(post.getId())
                .authorId(post.getAuthorId())
                .authorName(post.getAuthorName())
                .authorAvatarUrl(post.getAuthorAvatarUrl())
                .title(post.getTitle())
                .content(post.getContent())
                .type(post.getType())
                .mediaUrls(post.getMediaUrls() != null ? new ArrayList<>(post.getMediaUrls()) : Collections.emptyList())
                .employer(post.getEmployer())
                .compensation(post.getCompensation())
                .location(post.getLocation())
                .expiryDate(post.getExpiryDate())
                .applicationsCount(post.getApplicationsCount() != null ? post.getApplicationsCount() : 0)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
