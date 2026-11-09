package com.truehunt.social.service;

import com.truehunt.social.dto.CreatePostRequest;
import com.truehunt.social.exception.BadRequestException;
import com.truehunt.social.exception.ResourceNotFoundException;
import com.truehunt.social.model.NotificationEntity;
import com.truehunt.social.model.PostEntity;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.repository.NotificationRepository;
import com.truehunt.social.repository.PostRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final NotificationRepository notificationRepository;
    private final ProfileService profileService;

    public PostService(PostRepository postRepository, NotificationRepository notificationRepository, ProfileService profileService) {
        this.postRepository = postRepository;
        this.notificationRepository = notificationRepository;
        this.profileService = profileService;
    }

    public List<Map<String, Object>> getPosts(String type, String userIdentity) {
        List<PostEntity> list = (type != null && !type.isBlank())
                ? postRepository.findAllByTypeIgnoreCaseOrderByCreatedAtDesc(type)
                : postRepository.findAllByOrderByCreatedAtDesc();

        return list.stream()
                .map(post -> post.toDto(userIdentity))
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getPostsByAuthorId(String authorId, String userIdentity) {
        List<PostEntity> list = postRepository.findAllByAuthorIdOrderByCreatedAtDesc(authorId);
        if (list.isEmpty() && "me".equalsIgnoreCase(authorId)) {
            list = postRepository.findAllByAuthorIdOrderByCreatedAtDesc("1000001");
        } else if (list.isEmpty() && "1000001".equalsIgnoreCase(authorId)) {
            list = postRepository.findAllByAuthorIdOrderByCreatedAtDesc("me");
        }
        return list.stream()
                .map(post -> post.toDto(userIdentity))
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getOpportunities(String userIdentity) {
        return postRepository.findAllByTypeIgnoreCaseOrderByCreatedAtDesc("opportunity").stream()
                .map(post -> post.toDto(userIdentity))
                .collect(Collectors.toList());
    }

    public Map<String, Object> getPostById(String id, String userIdentity) {
        Long numericId;
        try {
            numericId = Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new ResourceNotFoundException("Post", "id", id);
        }
        return postRepository.findById(numericId)
                .map(post -> post.toDto(userIdentity))
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));
    }

    @Transactional
    public Map<String, Object> createPost(CreatePostRequest req, String userEmail) {
        if (req == null || (req.getContent() == null || req.getContent().trim().isEmpty())) {
            throw new BadRequestException("Post content cannot be empty.");
        }
        ProfileEntity currentProfile = profileService.findCurrentProfile(userEmail);

        PostEntity post = new PostEntity();
        post.setAuthorId(currentProfile.getUserId() != null ? currentProfile.getUserId() : "me");
        post.setAuthorName(currentProfile.getName() != null ? currentProfile.getName() : "Cinema Professional");
        post.setAuthorAvatar(currentProfile.getProfileImageUrl());
        post.setAuthorHeadline(currentProfile.getPrimaryCraft() != null ? currentProfile.getPrimaryCraft() : "Film Industry Professional");
        post.setType(req.getType() != null ? req.getType().toLowerCase() : "post");
        post.setContent(req.getContent());
        post.setTitle(req.getTitle());
        if (req.getMedia() != null) {
            post.setMedia(req.getMedia());
            if (req.getMediaUrl() == null && req.getMedia().get("url") != null) {
                post.setMediaUrl(String.valueOf(req.getMedia().get("url")));
            }
            if (req.getMediaType() == null && req.getMedia().get("type") != null) {
                post.setMediaType(String.valueOf(req.getMedia().get("type")));
            }
        } else {
            post.setMediaUrl(req.getMediaUrl());
            post.setMediaType(req.getMediaType());
        }
        post.setCreatedAt(LocalDateTime.now());

        if (req.getOpportunityDetails() != null) {
            post.setOpportunityDetails(req.getOpportunityDetails());
        }

        PostEntity saved = postRepository.save(post);
        return saved.toDto();
    }

    @Transactional
    public void deletePost(String id) {
        Long numericId = parseLongId(id);
        if (numericId != null && postRepository.existsById(numericId)) {
            postRepository.deleteById(numericId);
        }
    }

    @Transactional
    public Map<String, Object> likePost(String id, String userIdentity) {
        String identity = userIdentity;
        if (identity == null || identity.isBlank()) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                identity = auth.getName();
            }
        }
        if (identity == null || identity.isBlank()) {
            identity = "pro@truehunt.in";
        }
        Long numericId = parseLongId(id);
        if (numericId == null) {
            throw new ResourceNotFoundException("Post", "id", id);
        }

        PostEntity post = postRepository.findById(numericId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));

        post.toggleLikeForUser(identity);
        postRepository.save(post);

        Map<String, Object> res = new HashMap<>();
        res.put("id", id);
        res.put("postId", id);
        res.put("liked", post.isLikedByUser(identity));
        res.put("likesCount", post.getLikesCount());
        return res;
    }

    @Transactional
    public Map<String, Object> commentOnPost(String id, String text, String userEmail) {
        if (text == null || text.trim().isEmpty()) {
            throw new BadRequestException("Comment text cannot be empty.");
        }
        Long numericId = parseLongId(id);
        if (numericId == null) {
            throw new ResourceNotFoundException("Post", "id", id);
        }

        PostEntity post = postRepository.findById(numericId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));

        String email = userEmail;
        if (email == null || email.isBlank()) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                email = auth.getName();
            }
        }
        if (email == null || email.isBlank()) {
            email = "pro@truehunt.in";
        }

        ProfileEntity profile = profileService.findCurrentProfile(email);
        String authorName = (profile != null && profile.getName() != null && !profile.getName().isBlank())
                ? profile.getName() : "Cinema Professional";

        post.addComment(authorName, text);
        postRepository.save(post);

        Map<String, Object> res = post.toDto(email);
        res.put("id", id);
        res.put("postId", id);
        res.put("commentsCount", post.getCommentsCount());
        res.put("comments", post.getComments());
        return res;
    }

    @Transactional
    public Map<String, Object> applyToOpportunity(String id) {
        Long numericId = parseLongId(id);
        if (numericId == null) {
            throw new ResourceNotFoundException("Opportunity", "id", id);
        }

        PostEntity post = postRepository.findById(numericId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity", "id", id));

        post.toggleApply();
        postRepository.save(post);

        notificationRepository.save(new NotificationEntity(
                "me",
                "Opportunity Application Sent",
                "Your application for \"" + (post.getTitle() != null ? post.getTitle() : "Casting Call") + "\" has been received.",
                "opportunity"
        ));

        Map<String, Object> res = new HashMap<>();
        res.put("id", id);
        res.put("postId", id);
        res.put("applied", post.isApplied());
        res.put("applicationsCount", post.getApplicationsCount());
        res.put("message", "Application submitted successfully.");
        return res;
    }

    private Long parseLongId(String id) {
        try {
            return Long.parseLong(id);
        } catch (Exception e) {
            return null;
        }
    }
}
