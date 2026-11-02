package com.truehunt.social.service;

import com.truehunt.social.dto.CreatePostRequest;
import com.truehunt.social.exception.BadRequestException;
import com.truehunt.social.exception.ResourceNotFoundException;
import com.truehunt.social.model.NotificationEntity;
import com.truehunt.social.model.PostEntity;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.repository.NotificationRepository;
import com.truehunt.social.repository.PostRepository;
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

    public List<Map<String, Object>> getPosts(String type) {
        List<PostEntity> list = (type != null && !type.isBlank())
                ? postRepository.findAllByTypeIgnoreCaseOrderByCreatedAtDesc(type)
                : postRepository.findAllByOrderByCreatedAtDesc();

        return list.stream()
                .map(PostEntity::toDto)
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getOpportunities() {
        return postRepository.findAllByTypeIgnoreCaseOrderByCreatedAtDesc("opportunity").stream()
                .map(PostEntity::toDto)
                .collect(Collectors.toList());
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
        post.setMediaUrl(req.getMediaUrl());
        post.setMediaType(req.getMediaType());
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
        if (numericId == null || !postRepository.existsById(numericId)) {
            throw new ResourceNotFoundException("Post", "id", id);
        }
        postRepository.deleteById(numericId);
    }

    @Transactional
    public Map<String, Object> likePost(String id) {
        Long numericId = parseLongId(id);
        if (numericId == null) {
            throw new ResourceNotFoundException("Post", "id", id);
        }

        PostEntity post = postRepository.findById(numericId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));

        post.toggleLike();
        postRepository.save(post);

        Map<String, Object> res = new HashMap<>();
        res.put("postId", id);
        res.put("liked", post.isLiked());
        res.put("likesCount", post.getLikesCount());
        return res;
    }

    @Transactional
    public Map<String, Object> commentOnPost(String id, String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new BadRequestException("Comment text cannot be empty.");
        }
        Long numericId = parseLongId(id);
        if (numericId == null) {
            throw new ResourceNotFoundException("Post", "id", id);
        }

        PostEntity post = postRepository.findById(numericId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));

        post.addComment("Cinema Professional", text);
        postRepository.save(post);

        Map<String, Object> comment = new HashMap<>();
        comment.put("id", "c-" + System.currentTimeMillis());
        comment.put("authorName", "Cinema Professional");
        comment.put("text", text);
        comment.put("createdAt", "Just now");
        comment.put("postId", id);
        comment.put("commentsCount", post.getCommentsCount());
        comment.put("comments", post.getComments());
        return comment;
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
        res.put("applied", post.isApplied());
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
