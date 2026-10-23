package com.truehunt.service;

import com.truehunt.dto.request.CreateOpportunityRequest;
import com.truehunt.dto.request.CreatePostRequest;
import com.truehunt.dto.response.PostResponse;
import com.truehunt.exception.ForbiddenException;
import com.truehunt.exception.ResourceNotFoundException;
import com.truehunt.exception.ValidationException;
import com.truehunt.model.MediaAttachment;
import com.truehunt.model.Post;
import com.truehunt.model.Profile;
import com.truehunt.repository.PostRepository;
import com.truehunt.repository.ProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private MediaStorageService mediaStorageService;

    @Transactional
    public PostResponse createPost(String authorId, CreatePostRequest request) {
        String type = request.getType() != null ? request.getType().toLowerCase().trim() : "post";
        if (!Arrays.asList("post", "article", "opportunity").contains(type)) {
            throw new ValidationException("Invalid post type: " + type + ". Must be 'post', 'article', or 'opportunity'.");
        }

        if ("article".equalsIgnoreCase(type) && (request.getTitle() == null || request.getTitle().isBlank())) {
            throw new ValidationException("Title is required for articles.", Collections.singletonMap("title", "Title is required for article."));
        }

        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new ValidationException("Content is required.", Collections.singletonMap("content", "Content is required."));
        }

        Post post = new Post();
        post.setAuthorId(authorId);
        post.setType(type);
        post.setTitle(request.getTitle() != null ? request.getTitle().trim() : null);
        post.setContent(request.getContent().trim());
        post.setAttachments(request.getAttachments() != null ? request.getAttachments() : new ArrayList<>());
        post.setVisibility(request.getVisibility() != null ? request.getVisibility().toLowerCase() : "public");

        post = postRepository.save(post);
        return toPostResponse(post);
    }

    @Transactional
    public PostResponse createOpportunity(String authorId, CreateOpportunityRequest request) {
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new ValidationException("Content is required.", Collections.singletonMap("content", "Content is required."));
        }

        Post post = new Post();
        post.setAuthorId(authorId);
        post.setType("opportunity");
        post.setTitle(request.getTitle() != null ? request.getTitle().trim() : null);
        post.setContent(request.getContent().trim());
        post.setAttachments(request.getAttachments() != null ? request.getAttachments() : new ArrayList<>());
        post.setVisibility(request.getVisibility() != null ? request.getVisibility().toLowerCase() : "public");

        post = postRepository.save(post);
        return toPostResponse(post);
    }

    @Transactional
    public PostResponse attachMedia(String postId, String currentUserId, MediaAttachment media) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with ID: " + postId));

        if (!post.getAuthorId().equals(currentUserId)) {
            throw new ForbiddenException("You are not authorized to modify this post.");
        }

        if (post.getAttachments() == null) {
            post.setAttachments(new ArrayList<>());
        }
        post.getAttachments().add(media);
        post = postRepository.save(post);
        return toPostResponse(post);
    }

    public List<PostResponse> getPostsFeed(String cursor, int limit) {
        int pageSize = Math.min(Math.max(limit, 1), 50);
        PageRequest pageRequest = PageRequest.of(0, pageSize);
        List<String> feedTypes = Arrays.asList("post", "article");

        List<Post> posts;
        if (cursor != null && !cursor.trim().isEmpty()) {
            Instant cursorTime = Instant.parse(cursor);
            posts = postRepository.findByTypeInAndCreatedAtBeforeOrderByCreatedAtDesc(feedTypes, cursorTime, pageRequest);
        } else {
            posts = postRepository.findByTypeInOrderByCreatedAtDesc(feedTypes, pageRequest);
        }

        return populateAuthorInfo(posts);
    }

    public List<PostResponse> getOpportunitiesFeed(String cursor, int limit) {
        int pageSize = Math.min(Math.max(limit, 1), 50);
        PageRequest pageRequest = PageRequest.of(0, pageSize);

        List<Post> opportunities;
        if (cursor != null && !cursor.trim().isEmpty()) {
            Instant cursorTime = Instant.parse(cursor);
            opportunities = postRepository.findByTypeAndCreatedAtBeforeOrderByCreatedAtDesc("opportunity", cursorTime, pageRequest);
        } else {
            opportunities = postRepository.findByTypeOrderByCreatedAtDesc("opportunity", pageRequest);
        }

        return populateAuthorInfo(opportunities);
    }

    private List<PostResponse> populateAuthorInfo(List<Post> posts) {
        if (posts.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> authorIds = posts.stream()
                .map(Post::getAuthorId)
                .distinct()
                .collect(Collectors.toList());

        Map<String, Profile> profileMap = profileRepository.findByUserIdIn(authorIds).stream()
                .collect(Collectors.toMap(Profile::getUserId, p -> p));

        List<PostResponse> responses = new ArrayList<>();
        for (Post p : posts) {
            PostResponse res = toPostResponse(p);
            Profile author = profileMap.get(p.getAuthorId());
            if (author != null) {
                res.setAuthorName(author.getName());
                res.setAuthorProfileImage(mediaStorageService.buildMediaUrl(author.getProfileImageKey()));
            }
            responses.add(res);
        }
        return responses;
    }

    public PostResponse toPostResponse(Post post) {
        PostResponse res = new PostResponse();
        res.setId(post.getId());
        res.setAuthorId(post.getAuthorId());
        res.setType(post.getType());
        res.setTitle(post.getTitle());
        res.setContent(post.getContent());
        res.setAttachments(post.getAttachments());
        res.setVisibility(post.getVisibility());
        res.setCreatedAt(post.getCreatedAt());
        res.setUpdatedAt(post.getUpdatedAt());

        profileRepository.findByUserId(post.getAuthorId()).ifPresent(author -> {
            res.setAuthorName(author.getName());
            res.setAuthorProfileImage(mediaStorageService.buildMediaUrl(author.getProfileImageKey()));
        });

        return res;
    }
}
