package com.truehunt.social.service;

import com.truehunt.social.dto.CreatePostRequest;
import com.truehunt.social.exception.BadRequestException;
import com.truehunt.social.exception.ResourceNotFoundException;
import com.truehunt.social.exception.UnauthorizedException;
import com.truehunt.social.model.NotificationEntity;
import com.truehunt.social.model.OpportunityApplicationEntity;
import com.truehunt.social.model.PostEntity;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.repository.NotificationRepository;
import com.truehunt.social.repository.OpportunityApplicationRepository;
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
    private final OpportunityApplicationRepository opportunityApplicationRepository;
    private final NotificationRepository notificationRepository;
    private final ProfileService profileService;

    public PostService(
            PostRepository postRepository,
            OpportunityApplicationRepository opportunityApplicationRepository,
            NotificationRepository notificationRepository,
            ProfileService profileService
    ) {
        this.postRepository = postRepository;
        this.opportunityApplicationRepository = opportunityApplicationRepository;
        this.notificationRepository = notificationRepository;
        this.profileService = profileService;
    }

    public List<Map<String, Object>> getPosts(String type, String userIdentity) {
        List<PostEntity> list = (type != null && !type.isBlank())
                ? postRepository.findAllByTypeIgnoreCaseOrderByCreatedAtDesc(type)
                : postRepository.findAllByOrderByCreatedAtDesc();

        return list.stream()
                .map(post -> toUserAwareDto(post, userIdentity))
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
                .map(post -> toUserAwareDto(post, userIdentity))
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getOpportunities(String userIdentity) {
        return postRepository.findAllByTypeIgnoreCaseOrderByCreatedAtDesc("opportunity").stream()
                .map(post -> toUserAwareDto(post, userIdentity))
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
                .map(post -> toUserAwareDto(post, userIdentity))
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));
    }

    @Transactional
    public Map<String, Object> createPost(CreatePostRequest req, String userEmail) {
        boolean hasTextContent = req != null && req.getContent() != null && !req.getContent().trim().isEmpty();
        boolean hasMedia = req != null && (
                req.getMedia() != null && req.getMedia().get("url") != null
                        && !String.valueOf(req.getMedia().get("url")).isBlank()
                        || req.getMediaUrl() != null && !req.getMediaUrl().isBlank()
        );
        if (req == null || (!hasTextContent && !hasMedia)) {
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
        post.setInvestorProfileId(req.getInvestorProfileId());

        if (req.getOpportunityDetails() != null) {
            post.setOpportunityDetails(req.getOpportunityDetails());
        }

        PostEntity saved = postRepository.save(post);
        return saved.toDto();
    }

    @Transactional
    public void deletePost(String id, String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new UnauthorizedException("Sign in to delete this post.");
        }
        Long numericId = parseLongId(id);
        if (numericId == null) {
            throw new ResourceNotFoundException("Post", "id", id);
        }
        PostEntity post = postRepository.findById(numericId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));
        ProfileEntity currentProfile = profileService.findCurrentProfile(userEmail);
        String authorId = post.getAuthorId() == null ? "" : post.getAuthorId().trim();
        boolean isOwner = authorId.equalsIgnoreCase(userEmail.trim())
                || (currentProfile.getUserId() != null
                && authorId.equalsIgnoreCase(currentProfile.getUserId().trim()))
                || (currentProfile.getEmail() != null
                && authorId.equalsIgnoreCase(currentProfile.getEmail().trim()));
        if (!isOwner) {
            throw new UnauthorizedException("You can only delete posts that you created.");
        }
        postRepository.delete(post);
    }

    @Transactional
    public boolean deletePostIfPresent(String id, String userEmail) {
        Long numericId = parseLongId(id);
        if (numericId == null || !postRepository.existsById(numericId)) {
            return false;
        }
        deletePost(id, userEmail);
        return true;
    }

    @Transactional
    public Map<String, Object> updateOpportunity(String id, CreatePostRequest req, String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new UnauthorizedException("Sign in to edit this opportunity.");
        }
        if (req == null || req.getOpportunityDetails() == null) {
            throw new BadRequestException("Opportunity details are required.");
        }

        Long numericId = parseLongId(id);
        if (numericId == null) {
            throw new ResourceNotFoundException("Opportunity", "id", id);
        }
        PostEntity post = postRepository.findById(numericId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity", "id", id));
        if (!"opportunity".equalsIgnoreCase(post.getType())) {
            throw new ResourceNotFoundException("Opportunity", "id", id);
        }

        ProfileEntity currentProfile = profileService.findCurrentProfile(userEmail);
        String authorId = post.getAuthorId() == null ? "" : post.getAuthorId().trim();
        boolean isOwner = authorId.equalsIgnoreCase(userEmail.trim())
                || (currentProfile.getUserId() != null
                && authorId.equalsIgnoreCase(currentProfile.getUserId().trim()))
                || (currentProfile.getEmail() != null
                && authorId.equalsIgnoreCase(currentProfile.getEmail().trim()));
        if (!isOwner) {
            throw new UnauthorizedException("You can only edit opportunities that you posted.");
        }

        post.setTitle(req.getTitle());
        post.setContent(req.getContent());
        post.setOpportunityDetails(req.getOpportunityDetails());
        return toUserAwareDto(postRepository.save(post), userEmail);
    }

    @Transactional
    public Map<String, Object> updatePost(String id, CreatePostRequest req, String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new UnauthorizedException("Sign in to edit this post.");
        }
        if (req == null) {
            throw new BadRequestException("Post changes are required.");
        }

        Long numericId = parseLongId(id);
        if (numericId == null) {
            throw new ResourceNotFoundException("Post", "id", id);
        }
        PostEntity post = postRepository.findById(numericId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));

        ProfileEntity currentProfile = profileService.findCurrentProfile(userEmail);
        String authorId = post.getAuthorId() == null ? "" : post.getAuthorId().trim();
        boolean isOwner = authorId.equalsIgnoreCase(userEmail.trim())
                || (currentProfile.getUserId() != null
                && authorId.equalsIgnoreCase(currentProfile.getUserId().trim()))
                || (currentProfile.getEmail() != null
                && authorId.equalsIgnoreCase(currentProfile.getEmail().trim()));
        if (!isOwner) {
            throw new UnauthorizedException("You can only edit posts that you created.");
        }

        boolean hasMedia = req.getMedia() != null && req.getMedia().get("url") != null
                && !String.valueOf(req.getMedia().get("url")).isBlank();
        boolean hasMediaUrl = req.getMediaUrl() != null && !req.getMediaUrl().isBlank();
        if ((req.getContent() == null || req.getContent().isBlank())
                && !hasMedia && !hasMediaUrl && post.getMedia() == null) {
            throw new BadRequestException("A post must include text or media.");
        }

        post.setTitle(req.getTitle());
        post.setContent(req.getContent());
        if (hasMedia) {
            post.setMedia(req.getMedia());
            post.setMediaUrl(String.valueOf(req.getMedia().get("url")));
            Object mediaType = req.getMedia().get("kind");
            if (mediaType == null) mediaType = req.getMedia().get("type");
            post.setMediaType(mediaType == null ? null : String.valueOf(mediaType));
        } else if (hasMediaUrl) {
            post.setMediaUrl(req.getMediaUrl());
            post.setMediaType(req.getMediaType());
        }

        return toUserAwareDto(postRepository.save(post), userEmail);
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

        String identity = resolveCommentIdentity(userEmail);
        ProfileEntity authorProfile = profileService.findCurrentProfile(identity);

        post.addComment(
                resolveCommentAuthorName(authorProfile),
                resolveCommentAuthorAvatar(authorProfile),
                identity,
                text
        );
        postRepository.save(post);

        Map<String, Object> res = toUserAwareDto(post, identity);
        res.put("id", id);
        res.put("postId", id);
        res.put("commentsCount", post.getCommentsCount());
        return res;
    }

    @Transactional
    public Map<String, Object> deleteComment(String postId, String commentId, String userIdentity) {
        Long numericId = parseLongId(postId);
        if (numericId == null) {
            throw new ResourceNotFoundException("Post", "id", postId);
        }

        PostEntity post = postRepository.findById(numericId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
        Set<String> callerIdentities = resolveCommentOwnerIdentities(userIdentity);
        Map<String, Object> comment = post.getComments().stream()
                .filter(item -> Objects.equals(String.valueOf(item.get("id")), commentId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        if (!isPostOwner(post, callerIdentities)
                && !callerIdentities.contains(normalizeCommentOwnerIdentity(comment.get("authorId")))) {
            throw new UnauthorizedException("You can only delete your own comments.");
        }
        if (!post.deleteComment(commentId)) {
            throw new ResourceNotFoundException("Comment", "id", commentId);
        }

        return toUserAwareDto(postRepository.save(post), userIdentity);
    }

    @Transactional
    public Map<String, Object> toggleCommentLike(String postId, String commentId, String userIdentity) {
        Long numericId = parseLongId(postId);
        if (numericId == null) {
            throw new ResourceNotFoundException("Post", "id", postId);
        }

        PostEntity post = postRepository.findById(numericId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
        String identity = resolveCommentIdentity(userIdentity);
        if (!post.toggleCommentLike(commentId, identity)) {
            throw new ResourceNotFoundException("Comment", "id", commentId);
        }

        return toUserAwareDto(postRepository.save(post), identity);
    }

    @Transactional
    public Map<String, Object> replyToComment(String postId, String commentId, String text, String userIdentity) {
        if (text == null || text.trim().isEmpty()) {
            throw new BadRequestException("Reply text cannot be empty.");
        }
        Long numericId = parseLongId(postId);
        if (numericId == null) {
            throw new ResourceNotFoundException("Post", "id", postId);
        }

        PostEntity post = postRepository.findById(numericId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
        String identity = resolveCommentIdentity(userIdentity);
        ProfileEntity authorProfile = profileService.findCurrentProfile(identity);
        if (!post.addReply(
                commentId,
                resolveCommentAuthorName(authorProfile),
                resolveCommentAuthorAvatar(authorProfile),
                identity,
                text.trim()
        )) {
            throw new ResourceNotFoundException("Comment", "id", commentId);
        }

        return toUserAwareDto(postRepository.save(post), identity);
    }

    @Transactional
    public Map<String, Object> deleteCommentReply(
            String postId,
            String commentId,
            String replyId,
            String userIdentity
    ) {
        Long numericId = parseLongId(postId);
        if (numericId == null) {
            throw new ResourceNotFoundException("Post", "id", postId);
        }

        PostEntity post = postRepository.findById(numericId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
        Set<String> callerIdentities = resolveCommentOwnerIdentities(userIdentity);
        Map<String, Object> comment = post.getComments().stream()
                .filter(item -> Objects.equals(String.valueOf(item.get("id")), commentId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));
        Object storedReplies = comment.get("replies");
        Map<?, ?> reply = null;
        if (storedReplies instanceof List<?> replies) {
            for (Object item : replies) {
                if (item instanceof Map<?, ?> candidate
                        && Objects.equals(String.valueOf(candidate.get("id")), replyId)) {
                    reply = candidate;
                    break;
                }
            }
        }
        if (reply == null) {
            throw new ResourceNotFoundException("Reply", "id", replyId);
        }

        if (!isPostOwner(post, callerIdentities)
                && !callerIdentities.contains(normalizeCommentOwnerIdentity(reply.get("authorId")))) {
            throw new UnauthorizedException("You can only delete your own replies.");
        }
        if (!post.deleteReply(commentId, replyId)) {
            throw new ResourceNotFoundException("Reply", "id", replyId);
        }

        return toUserAwareDto(postRepository.save(post), userIdentity);
    }

    private Set<String> resolveCommentOwnerIdentities(String userIdentity) {
        if (userIdentity == null || userIdentity.isBlank()) {
            throw new UnauthorizedException("Sign in to delete comments.");
        }

        ProfileEntity profile = profileService.findCurrentProfile(userIdentity);
        Set<String> identities = new HashSet<>();
        identities.add(normalizeCommentOwnerIdentity(userIdentity));
        if (profile != null) {
            identities.add(normalizeCommentOwnerIdentity(profile.getUserId()));
            identities.add(normalizeCommentOwnerIdentity(profile.getEmail()));
        }
        identities.remove("");
        return identities;
    }

    private boolean isPostOwner(PostEntity post, Set<String> callerIdentities) {
        return callerIdentities.contains(normalizeCommentOwnerIdentity(post.getAuthorId()));
    }

    private String normalizeCommentOwnerIdentity(Object identity) {
        return identity == null ? "" : String.valueOf(identity).trim().toLowerCase(Locale.ROOT);
    }

    private String resolveCommentIdentity(String userIdentity) {
        if (userIdentity != null && !userIdentity.isBlank()) return userIdentity.trim();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "pro@truehunt.in";
    }

    private String resolveCommentAuthorName(ProfileEntity profile) {
        return profile != null && profile.getName() != null && !profile.getName().isBlank()
                ? profile.getName()
                : "Cinema Professional";
    }

    private String resolveCommentAuthorAvatar(ProfileEntity profile) {
        return profile != null && profile.getProfileImageUrl() != null
                ? profile.getProfileImageUrl()
                : "";
    }

    @Transactional
    public Map<String, Object> applyToOpportunity(String id, String applicantIdentity) {
        String identity = normalizeApplicantIdentity(applicantIdentity);
        Long numericId = parseLongId(id);
        if (numericId == null) {
            throw new ResourceNotFoundException("Opportunity", "id", id);
        }

        PostEntity post = postRepository.findById(numericId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity", "id", id));

        if (opportunityApplicationRepository.existsByPostIdAndApplicantIdentity(numericId, identity)) {
            return applicationResponse(id, post, true, "You have already applied for this opportunity.");
        }

        opportunityApplicationRepository.save(new OpportunityApplicationEntity(numericId, identity));
        post.setApplicationsCount(post.getApplicationsCount() + 1);
        postRepository.save(post);

        notificationRepository.save(new NotificationEntity(
                identity,
                "Opportunity Application Sent",
                "Your application for \"" + (post.getTitle() != null ? post.getTitle() : "Casting Call") + "\" has been received.",
                "opportunity"
        ));

        return applicationResponse(id, post, true, "Application submitted successfully.");
    }

    @Transactional
    public Map<String, Object> cancelOpportunityApplication(String id, String applicantIdentity) {
        String identity = normalizeApplicantIdentity(applicantIdentity);
        Long numericId = parseLongId(id);
        if (numericId == null) {
            throw new ResourceNotFoundException("Opportunity", "id", id);
        }

        PostEntity post = postRepository.findById(numericId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity", "id", id));
        Optional<OpportunityApplicationEntity> application =
                opportunityApplicationRepository.findByPostIdAndApplicantIdentity(numericId, identity);

        if (application.isPresent()) {
            opportunityApplicationRepository.delete(application.get());
            post.setApplicationsCount(Math.max(0, post.getApplicationsCount() - 1));
            postRepository.save(post);
        }

        return applicationResponse(id, post, false, "Application cancelled successfully.");
    }

    private String normalizeApplicantIdentity(String applicantIdentity) {
        if (applicantIdentity == null || applicantIdentity.isBlank()) {
            throw new UnauthorizedException("Sign in to apply for this opportunity.");
        }
        String identity = applicantIdentity.trim().toLowerCase(Locale.ROOT);
        if (identity.length() > 255) {
            throw new BadRequestException("Applicant identity is too long.");
        }
        return identity;
    }

    private Map<String, Object> toUserAwareDto(PostEntity post, String userIdentity) {
        Map<String, Object> dto = post.toDto(userIdentity);
        if ("opportunity".equalsIgnoreCase(post.getType())) {
            String identity = userIdentity == null ? "" : userIdentity.trim().toLowerCase(Locale.ROOT);
            dto.put("applied", !identity.isEmpty()
                    && opportunityApplicationRepository.existsByPostIdAndApplicantIdentity(post.getId(), identity));
        }
        return dto;
    }

    private Map<String, Object> applicationResponse(
            String id,
            PostEntity post,
            boolean applied,
            String message
    ) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("postId", id);
        response.put("applied", applied);
        response.put("applicationsCount", post.getApplicationsCount());
        response.put("message", message);
        return response;
    }

    private Long parseLongId(String id) {
        try {
            return Long.parseLong(id);
        } catch (Exception e) {
            return null;
        }
    }
}
