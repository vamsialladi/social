package com.truehunt.social.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.truehunt.social.dto.CreatePostRequest;
import com.truehunt.social.dto.ProfileDto;
import com.truehunt.social.exception.ApiException;
import com.truehunt.social.exception.BadRequestException;
import com.truehunt.social.exception.ResourceNotFoundException;
import com.truehunt.social.exception.UnauthorizedException;
import com.truehunt.social.model.*;
import com.truehunt.social.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PostService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final PostRepository postRepository;
    private final OpportunityApplicationRepository opportunityApplicationRepository;
    private final NotificationRepository notificationRepository;
    private final ProfileService profileService;
    private final SavedPostRepository savedPostRepository;
    private final PostReportRepository postReportRepository;
    private final HiddenContentRepository hiddenContentRepository;
    private final SettingsService settingsService;
    private final UserProfileRelationshipRepository relationshipRepository;

    public PostService(
            PostRepository postRepository,
            OpportunityApplicationRepository opportunityApplicationRepository,
            NotificationRepository notificationRepository,
            ProfileService profileService) {
        this(postRepository, opportunityApplicationRepository, notificationRepository, profileService,
                null, null, null, null, null);
    }

    @Autowired
    public PostService(
            PostRepository postRepository,
            OpportunityApplicationRepository opportunityApplicationRepository,
            NotificationRepository notificationRepository,
            ProfileService profileService,
            SavedPostRepository savedPostRepository,
            PostReportRepository postReportRepository,
            HiddenContentRepository hiddenContentRepository,
            SettingsService settingsService,
            UserProfileRelationshipRepository relationshipRepository) {
        this.postRepository = postRepository;
        this.opportunityApplicationRepository = opportunityApplicationRepository;
        this.notificationRepository = notificationRepository;
        this.profileService = profileService;
        this.savedPostRepository = savedPostRepository;
        this.postReportRepository = postReportRepository;
        this.hiddenContentRepository = hiddenContentRepository;
        this.settingsService = settingsService;
        this.relationshipRepository = relationshipRepository;
    }

    public List<Map<String, Object>> getPosts(String type, String userIdentity) {
        List<PostEntity> list = (type != null && !type.isBlank())
                ? postRepository.findAllByTypeIgnoreCaseOrderByCreatedAtDesc(type)
                : postRepository.findAllByOrderByCreatedAtDesc();

        boolean callerIsAdmin = isCurrentUserAdmin();
        Set<String> callerIdentities = callerIsAdmin ? Set.of() : resolveOptionalCallerIdentities(userIdentity);
        String callerKey = resolveOptionalStorageKey(userIdentity);
        Set<String> hiddenPostIds = new HashSet<>();
        Set<String> hiddenAuthorIds = new HashSet<>();
        if (!callerIsAdmin && hiddenContentRepository != null && !callerKey.isEmpty()) {
            for (HiddenContentEntity hc : hiddenContentRepository.findAllByUserEmailIgnoreCase(callerKey)) {
                if ("POST".equalsIgnoreCase(hc.getTargetType()) && hc.getTargetId() != null) {
                    hiddenPostIds.add(hc.getTargetId().trim());
                } else if ("AUTHOR".equalsIgnoreCase(hc.getTargetType()) && hc.getTargetId() != null) {
                    hiddenAuthorIds.add(hc.getTargetId().trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        Set<String> blockedIdentities = (!callerIsAdmin && settingsService != null && !callerKey.isEmpty())
                ? settingsService.getBlockedIdentitySet(userIdentity)
                : Set.of();
        Set<String> connectedTargetIds = (!callerIsAdmin && relationshipRepository != null
                && !callerIdentities.isEmpty())
                        ? resolveConnectedTargetIds(callerIdentities)
                        : Set.of();

        return list.stream()
                .filter(post -> (type != null && !type.isBlank())
                        || !"casting".equalsIgnoreCase(post.getType()))
                .filter(post -> callerIsAdmin || isPostVisibleToCaller(
                        post, callerIdentities, connectedTargetIds, hiddenPostIds, hiddenAuthorIds, blockedIdentities))
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
        boolean callerIsAdmin = isCurrentUserAdmin();
        Set<String> callerIdentities = callerIsAdmin ? Set.of() : resolveOptionalCallerIdentities(userIdentity);
        Set<String> connectedTargetIds = (!callerIsAdmin && relationshipRepository != null
                && !callerIdentities.isEmpty())
                        ? resolveConnectedTargetIds(callerIdentities)
                        : Set.of();
        return list.stream()
                .filter(post -> !"casting".equalsIgnoreCase(post.getType()))
                .filter(post -> callerIsAdmin || isAudienceAllowed(post, callerIdentities, connectedTargetIds))
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
        PostEntity post = postRepository.findById(numericId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));
        if (!isCurrentUserAdmin()) {
            Set<String> callerIdentities = resolveOptionalCallerIdentities(userIdentity);
            Set<String> connectedTargetIds = (relationshipRepository != null && !callerIdentities.isEmpty())
                    ? resolveConnectedTargetIds(callerIdentities)
                    : Set.of();
            if (!isAudienceAllowed(post, callerIdentities, connectedTargetIds)) {
                throw new ResourceNotFoundException("Post", "id", id);
            }
        }
        return toUserAwareDto(post, userIdentity);
    }

    private ProfileEntity findCurrentProfileSafely(String userEmail) {
        if (profileService == null || userEmail == null || userEmail.isBlank()) {
            return null;
        }
        try {
            return profileService.findCurrentProfile(userEmail);
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional
    public Map<String, Object> createPost(CreatePostRequest req, String userEmail) {
        boolean hasTextContent = req != null && req.getContent() != null && !req.getContent().trim().isEmpty();
        boolean hasMedia = req != null && (req.getMedia() != null && req.getMedia().get("url") != null
                && !String.valueOf(req.getMedia().get("url")).isBlank()
                || req.getMediaUrl() != null && !req.getMediaUrl().isBlank());
        if (req == null || (!hasTextContent && !hasMedia)) {
            throw new BadRequestException("Post content cannot be empty.");
        }
        ProfileEntity currentProfile = findCurrentProfileSafely(userEmail);
        boolean directProfile = isDirectProfileMatch(currentProfile, userEmail);

        PostEntity post = new PostEntity();
        if (directProfile && currentProfile != null) {
            post.setAuthorId(currentProfile.getUserId() != null ? currentProfile.getUserId() : "me");
            post.setAuthorName(currentProfile.getName() != null ? currentProfile.getName() : "Cinema Professional");
            post.setAuthorAvatar(currentProfile.getProfileImageUrl());
            post.setAuthorHeadline(currentProfile.getPrimaryCraft() != null ? currentProfile.getPrimaryCraft()
                    : "Film Industry Professional");
        } else {
            post.setAuthorId(userEmail != null ? userEmail.trim().toLowerCase(Locale.ROOT) : "me");
            post.setAuthorName("Cinema Professional");
            post.setAuthorAvatar(null);
            post.setAuthorHeadline("Film Industry Professional");
        }
        post.setType(req.getType() != null ? req.getType().toLowerCase() : "post");
        boolean explicitAudience = req.getAudience() != null && !req.getAudience().isBlank();
        post.setAudience(explicitAudience ? normalizeAudience(req.getAudience()) : "public");
        post.setAudienceOverride(explicitAudience);
        post.setContent(req.getContent());
        post.setTitle(req.getTitle());
        if (req.getMedia() != null) {
            post.setMedia(req.getMedia());
            if (req.getMediaUrl() == null && req.getMedia().get("url") != null) {
                post.setMediaUrl(String.valueOf(req.getMedia().get("url")));
            }
            if (req.getMediaType() == null) {
                Object mt = req.getMedia().get("kind");
                if (mt == null)
                    mt = req.getMedia().get("type");
                if (mt == null)
                    mt = req.getMedia().get("contentType");
                if (mt != null) {
                    post.setMediaType(String.valueOf(mt));
                }
            } else {
                post.setMediaType(req.getMediaType());
            }
        } else {
            post.setMediaUrl(req.getMediaUrl());
            post.setMediaType(req.getMediaType());
        }
        if (!explicitAudience
                && settingsService != null
                && settingsService.isPostRestrictedByAuthorMediaVisibility(post)) {
            post.setAudience("private");
        }
        post.setCreatedAt(LocalDateTime.now());
        post.setInvestorProfileId(req.getInvestorProfileId());

        if (req.getOpportunityDetails() != null) {
            post.setOpportunityDetails(req.getOpportunityDetails());
        }

        PostEntity saved = postRepository.save(post);

        if (notificationRepository != null && !"casting".equalsIgnoreCase(saved.getType())) {
            String authorName = saved.getAuthorName() != null && !saved.getAuthorName().isBlank()
                    ? saved.getAuthorName()
                    : "Your connection";
            String postTitle = saved.getTitle() != null && !saved.getTitle().isBlank()
                    ? saved.getTitle()
                    : (saved.getContent() != null && !saved.getContent().isBlank()
                            ? (saved.getContent().length() > 40 ? saved.getContent().substring(0, 40) + "…"
                                    : saved.getContent())
                            : "a new update");
            boolean isOpportunity = "opportunity".equalsIgnoreCase(saved.getType());
            String notifTitle = "New " + (isOpportunity ? "Opportunity" : "Post") + " from " + authorName;
            String notifMsg = isOpportunity
                    ? authorName + " shared an opportunity: " + postTitle
                    : authorName + " posted: " + postTitle;
            String notifType = isOpportunity ? "opportunity_post" : "post";

            Set<String> connections = findConnectedUserIds(userEmail, currentProfile);
            for (String connId : connections) {
                if (isPostingAndCommentingAllowed(connId, null)) {
                    notificationRepository.save(new NotificationEntity(connId, notifTitle, notifMsg, notifType));
                }
            }
        }

        return toUserAwareDto(saved, userEmail);
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
        ProfileEntity currentProfile = findCurrentProfileSafely(userEmail);
        String authorId = post.getAuthorId() == null ? "" : post.getAuthorId().trim();
        boolean directProfile = currentProfile != null && isDirectProfileMatch(currentProfile, userEmail);
        boolean isOwner = authorId.equalsIgnoreCase(userEmail.trim())
                || (directProfile && currentProfile.getUserId() != null
                        && authorId.equalsIgnoreCase(currentProfile.getUserId().trim()))
                || (directProfile && currentProfile.getEmail() != null
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

        ProfileEntity currentProfile = findCurrentProfileSafely(userEmail);
        String authorId = post.getAuthorId() == null ? "" : post.getAuthorId().trim();
        boolean directProfile = currentProfile != null && isDirectProfileMatch(currentProfile, userEmail);
        boolean isOwner = authorId.equalsIgnoreCase(userEmail.trim())
                || (directProfile && currentProfile.getUserId() != null
                        && authorId.equalsIgnoreCase(currentProfile.getUserId().trim()))
                || (directProfile && currentProfile.getEmail() != null
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

        ProfileEntity currentProfile = findCurrentProfileSafely(userEmail);
        String authorId = post.getAuthorId() == null ? "" : post.getAuthorId().trim();
        boolean directProfile = currentProfile != null && isDirectProfileMatch(currentProfile, userEmail);
        boolean isOwner = authorId.equalsIgnoreCase(userEmail.trim())
                || (directProfile && currentProfile.getUserId() != null
                        && authorId.equalsIgnoreCase(currentProfile.getUserId().trim()))
                || (directProfile && currentProfile.getEmail() != null
                        && authorId.equalsIgnoreCase(currentProfile.getEmail().trim()));
        if (!isOwner) {
            throw new UnauthorizedException("You can only edit posts that you created.");
        }

        boolean hasMedia = req.getMedia() != null && req.getMedia().get("url") != null
                && !String.valueOf(req.getMedia().get("url")).isBlank();
        boolean hasMediaUrl = req.getMediaUrl() != null && !req.getMediaUrl().isBlank();
        String effectiveContent = req.getContent() != null ? req.getContent() : post.getContent();
        if ((effectiveContent == null || effectiveContent.isBlank())
                && !hasMedia && !hasMediaUrl && post.getMedia() == null) {
            throw new BadRequestException("A post must include text or media.");
        }

        if (req.getTitle() != null) {
            post.setTitle(req.getTitle());
        }
        if (req.getContent() != null) {
            post.setContent(req.getContent());
        }
        if (req.getOpportunityDetails() != null) {
            post.setOpportunityDetails(req.getOpportunityDetails());
        }
        if (req.getAudience() != null && !req.getAudience().isBlank()) {
            post.setAudience(normalizeAudience(req.getAudience()));
            post.setAudienceOverride(true);
        }
        if (hasMedia) {
            post.setMedia(req.getMedia());
            post.setMediaUrl(String.valueOf(req.getMedia().get("url")));
            Object mediaType = req.getMedia().get("kind");
            if (mediaType == null)
                mediaType = req.getMedia().get("type");
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

        boolean isAuthor = isAuthorOfPost(post, identity);
        boolean likeCountEnabled = settingsService == null || settingsService.isLikeCountEnabledForAuthor(post.getAuthorId());

        Map<String, Object> res = new HashMap<>();
        res.put("id", id);
        res.put("postId", id);
        res.put("liked", post.isLikedByUser(identity));
        res.put("isAuthor", isAuthor);
        if (isAuthor || likeCountEnabled) {
            res.put("likesCount", post.getLikesCount());
            res.put("showLikesCount", true);
            res.put("likeCountDisabled", false);
        } else {
            res.put("likesCount", 0);
            res.put("showLikesCount", false);
            res.put("likeCountDisabled", true);
        }
        return res;
    }

    public List<Map<String, Object>> getPostLikes(String id, String userIdentity) {
        Long numericId = parseLongId(id);
        if (numericId == null) {
            throw new ResourceNotFoundException("Post", "id", id);
        }
        PostEntity post = postRepository.findById(numericId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));
        boolean isAuthor = isAuthorOfPost(post, userIdentity);
        boolean likeCountEnabled = settingsService == null || settingsService.isLikeCountEnabledForAuthor(post.getAuthorId());
        if (!isAuthor && !likeCountEnabled) {
            return Collections.emptyList();
        }

        boolean callerIsAdmin = isCurrentUserAdmin();
        Set<String> callerIdentities = callerIsAdmin ? Set.of() : resolveOptionalCallerIdentities(userIdentity);
        String callerKey = resolveOptionalStorageKey(userIdentity);
        Set<String> hiddenPostIds = new HashSet<>();
        Set<String> hiddenAuthorIds = new HashSet<>();
        if (!callerIsAdmin && hiddenContentRepository != null && !callerKey.isEmpty()) {
            for (HiddenContentEntity hiddenContent : hiddenContentRepository.findAllByUserEmailIgnoreCase(callerKey)) {
                if ("POST".equalsIgnoreCase(hiddenContent.getTargetType()) && hiddenContent.getTargetId() != null) {
                    hiddenPostIds.add(hiddenContent.getTargetId().trim());
                } else if ("AUTHOR".equalsIgnoreCase(hiddenContent.getTargetType()) && hiddenContent.getTargetId() != null) {
                    hiddenAuthorIds.add(hiddenContent.getTargetId().trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        Set<String> blockedIdentities = (!callerIsAdmin && settingsService != null && !callerKey.isEmpty())
                ? settingsService.getBlockedIdentitySet(userIdentity)
                : Set.of();
        Set<String> connectedTargetIds = (!callerIsAdmin && relationshipRepository != null
                && !callerIdentities.isEmpty())
                        ? resolveConnectedTargetIds(callerIdentities)
                        : Set.of();
        if (!callerIsAdmin && !isPostVisibleToCaller(
                post, callerIdentities, connectedTargetIds, hiddenPostIds, hiddenAuthorIds, blockedIdentities)) {
            throw new ResourceNotFoundException("Post", "id", id);
        }

        List<Map<String, Object>> likers = new ArrayList<>();
        Set<String> includedProfileIds = new HashSet<>();
        for (String likerIdentity : post.likedByUserIdentities()) {
            try {
                ProfileDto profile = profileService.getProfileById(likerIdentity, userIdentity);
                String profileId = profile.getId() != null ? profile.getId() : profile.getUserId();
                String normalizedProfileId = profileId == null ? "" : profileId.trim().toLowerCase(Locale.ROOT);
                if (normalizedProfileId.isEmpty() || !includedProfileIds.add(normalizedProfileId)) {
                    continue;
                }
                Map<String, Object> liker = new LinkedHashMap<>();
                liker.put("id", profileId);
                liker.put("userId", profile.getUserId());
                liker.put("name", profile.getName());
                liker.put("headline", profile.getHeadline());
                liker.put("role", profile.getRole());
                liker.put("profileImageUrl", profile.getProfileImageUrl());
                liker.put("avatar", profile.getAvatar());
                liker.put("isConnected", Boolean.TRUE.equals(profile.getIsConnected()));
                liker.put("isPending", Boolean.TRUE.equals(profile.getIsPending()));
                liker.put("isOutgoingPending", Boolean.TRUE.equals(profile.getIsOutgoingPending()));
                liker.put("isIncomingPending", Boolean.TRUE.equals(profile.getIsIncomingPending()));
                likers.add(liker);
            } catch (ApiException exception) {
                if (exception.getStatus().is4xxClientError()) {
                    continue;
                }
                throw exception;
            }
        }
        return likers;
    }

    @Transactional
    public Map<String, Object> sharePost(String id, String userIdentity) {
        Long numericId = parseLongId(id);
        if (numericId == null) {
            throw new ResourceNotFoundException("Post", "id", id);
        }
        PostEntity post = postRepository.findById(numericId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));
        post.setSharesCount(post.getSharesCount() + 1);
        postRepository.save(post);

        boolean isAuthor = isAuthorOfPost(post, userIdentity);
        boolean shareCountEnabled = settingsService == null || settingsService.isShareCountEnabledForAuthor(post.getAuthorId());

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id", id);
        res.put("postId", id);
        res.put("allowed", true);
        res.put("isAuthor", isAuthor);
        if (isAuthor || shareCountEnabled) {
            res.put("sharesCount", post.getSharesCount());
            res.put("showSharesCount", true);
            res.put("shareCountDisabled", false);
        } else {
            res.put("sharesCount", 0);
            res.put("showSharesCount", false);
            res.put("shareCountDisabled", true);
        }
        return res;
    }

    // ==================== SAVED / BOOKMARKED POSTS ====================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSavedPosts(String userIdentity) {
        if (savedPostRepository == null) {
            return List.of();
        }
        String key = requireStorageKey(userIdentity, "Sign in to view saved posts.");
        List<SavedPostEntity> savedEntries = savedPostRepository.findAllByUserEmailIgnoreCaseOrderBySavedAtDesc(key);
        List<Map<String, Object>> result = new ArrayList<>();
        for (SavedPostEntity entry : savedEntries) {
            Optional<PostEntity> livePost = postRepository.findById(entry.getPostId());
            if (livePost.isPresent()) {
                Map<String, Object> dto = toUserAwareDto(livePost.get(), userIdentity);
                dto.put("saved", true);
                dto.put("ownerRemoved", false);
                dto.put("savedAt", entry.getSavedAt() != null ? entry.getSavedAt().toString() : "");
                result.add(dto);
            } else {
                Map<String, Object> snapshot = parseJsonMap(entry.getSnapshotJson());
                snapshot.putIfAbsent("id", String.valueOf(entry.getPostId()));
                snapshot.put("saved", true);
                snapshot.put("ownerRemoved", true);
                snapshot.put("savedAt", entry.getSavedAt() != null ? entry.getSavedAt().toString() : "");
                result.add(snapshot);
            }
        }
        return result;
    }

    @Transactional
    public Map<String, Object> savePost(String id, String userIdentity) {
        String key = requireStorageKey(userIdentity, "Sign in to save posts.");
        Long numericId = parseLongId(id);
        if (numericId == null) {
            throw new ResourceNotFoundException("Post", "id", id);
        }
        PostEntity post = postRepository.findById(numericId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));

        if (savedPostRepository != null) {
            SavedPostEntity entity = savedPostRepository.findByUserEmailIgnoreCaseAndPostId(key, numericId)
                    .orElseGet(() -> new SavedPostEntity(key, numericId, writeJson(post.toDto())));
            entity.setSnapshotJson(writeJson(post.toDto()));
            entity.setSavedAt(LocalDateTime.now());
            savedPostRepository.save(entity);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id", id);
        res.put("postId", id);
        res.put("saved", true);
        res.put("savedAt", LocalDateTime.now().toString());
        return res;
    }

    @Transactional
    public Map<String, Object> unsavePost(String id, String userIdentity) {
        String key = requireStorageKey(userIdentity, "Sign in to manage saved posts.");
        Long numericId = parseLongId(id);
        if (numericId != null && savedPostRepository != null) {
            savedPostRepository.findByUserEmailIgnoreCaseAndPostId(key, numericId)
                    .ifPresent(savedPostRepository::delete);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id", id);
        res.put("postId", id);
        res.put("saved", false);
        return res;
    }

    // ==================== REPORTED POSTS ====================

    @Transactional
    public Map<String, Object> reportPost(String id, Map<String, Object> req, String userIdentity) {
        String key = requireStorageKey(userIdentity, "Sign in to report posts.");
        String reason = req != null && req.get("reason") != null ? String.valueOf(req.get("reason")).trim() : "";
        if (reason.isEmpty()) {
            throw new BadRequestException("Report reason is required.");
        }

        PostEntity post = null;
        Long numericId = parseLongId(id);
        if (numericId != null) {
            post = postRepository.findById(numericId).orElse(null);
        }

        PostReportEntity report = new PostReportEntity();
        report.setReporterEmail(key);
        report.setPostId(id);
        report.setAuthorId(post != null ? post.getAuthorId() : stringValue(req, "authorId"));
        report.setAuthorName(
                post != null && post.getAuthorName() != null ? post.getAuthorName() : stringValue(req, "authorName"));
        report.setTitle(post != null && post.getTitle() != null ? post.getTitle() : stringValue(req, "title"));
        report.setContent(post != null && post.getContent() != null ? post.getContent() : stringValue(req, "content"));
        report.setReason(reason);
        report.setStatus("OPEN");
        report.setReportedAt(LocalDateTime.now());

        if (postReportRepository != null) {
            report = postReportRepository.save(report);
        }
        String reportAuthorId = report.getAuthorId();
        if (notificationRepository != null && reportAuthorId != null && !reportAuthorId.isBlank()
                && !reportAuthorId.trim().equalsIgnoreCase(key)) {
            notificationRepository.save(new NotificationEntity(
                    reportAuthorId,
                    "Your post was reported",
                    "A post you shared was reported by a community member and sent to the moderation team for review.",
                    "REPORT"));
        }
        return report.toDto();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMyReports(String userIdentity) {
        if (postReportRepository == null) {
            return List.of();
        }
        String key = requireStorageKey(userIdentity, "Sign in to view your reports.");
        return postReportRepository.findAllByReporterEmailIgnoreCaseOrderByReportedAtDesc(key).stream()
                .map(PostReportEntity::toDto)
                .toList();
    }

    @Transactional
    public Map<String, Object> deleteMyReport(String reportId, String userIdentity) {
        if (postReportRepository == null) {
            throw new ResourceNotFoundException("Report", "id", reportId);
        }
        String key = requireStorageKey(userIdentity, "Sign in to remove your reports.");
        Long numericId = parseLongId(reportId);
        if (numericId == null) {
            throw new ResourceNotFoundException("Report", "id", reportId);
        }
        PostReportEntity report = postReportRepository.findById(numericId)
                .filter(item -> item.getReporterEmail() != null
                        && item.getReporterEmail().equalsIgnoreCase(key))
                .orElseThrow(() -> new ResourceNotFoundException("Report", "id", reportId));
        postReportRepository.delete(report);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", reportId);
        response.put("deleted", true);
        return response;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllReports() {
        if (postReportRepository == null) {
            return List.of();
        }
        return postReportRepository.findAllByOrderByReportedAtDesc().stream()
                .map(PostReportEntity::toDto)
                .toList();
    }

    @Transactional
    public Map<String, Object> updateReportStatus(Long reportId, String status) {
        if (postReportRepository == null) {
            throw new ResourceNotFoundException("Report", "id", reportId);
        }
        PostReportEntity report = postReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", "id", reportId));
        String normalizedStatus = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("OPEN", "REVIEWED", "RESOLVED", "DISMISSED").contains(normalizedStatus)) {
            throw new BadRequestException("Invalid report status.");
        }
        report.setStatus(normalizedStatus);
        return postReportRepository.save(report).toDto();
    }

    // ==================== NOT INTERESTED / HIDDEN CONTENT ====================

    @Transactional
    public Map<String, Object> hidePost(String postId, String userIdentity) {
        String key = requireStorageKey(userIdentity, "Sign in to update feed preferences.");
        if (hiddenContentRepository != null && postId != null && !postId.isBlank()) {
            hiddenContentRepository
                    .findByUserEmailIgnoreCaseAndTargetTypeIgnoreCaseAndTargetIdIgnoreCase(key, "POST", postId.trim())
                    .orElseGet(() -> hiddenContentRepository.save(new HiddenContentEntity(key, "POST", postId.trim())));
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("postId", postId);
        res.put("hidden", true);
        return res;
    }

    @Transactional
    public Map<String, Object> unhidePost(String postId, String userIdentity) {
        String key = requireStorageKey(userIdentity, "Sign in to update feed preferences.");
        if (hiddenContentRepository != null && postId != null && !postId.isBlank()) {
            hiddenContentRepository
                    .findByUserEmailIgnoreCaseAndTargetTypeIgnoreCaseAndTargetIdIgnoreCase(key, "POST", postId.trim())
                    .ifPresent(hiddenContentRepository::delete);
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("postId", postId);
        res.put("hidden", false);
        return res;
    }

    @Transactional
    public Map<String, Object> hideAuthor(String authorId, String userIdentity) {
        String key = requireStorageKey(userIdentity, "Sign in to hide author posts.");
        if (authorId == null || authorId.isBlank()) {
            throw new BadRequestException("Author identifier is required.");
        }
        String normalizedAuthor = authorId.trim().toLowerCase(Locale.ROOT);
        if (hiddenContentRepository != null) {
            hiddenContentRepository
                    .findByUserEmailIgnoreCaseAndTargetTypeIgnoreCaseAndTargetIdIgnoreCase(key, "AUTHOR",
                            normalizedAuthor)
                    .orElseGet(() -> hiddenContentRepository
                            .save(new HiddenContentEntity(key, "AUTHOR", normalizedAuthor)));
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("authorId", normalizedAuthor);
        res.put("hidden", true);
        return res;
    }

    @Transactional
    public Map<String, Object> unhideAuthor(String authorId, String userIdentity) {
        String key = requireStorageKey(userIdentity, "Sign in to manage hidden authors.");
        if (hiddenContentRepository != null && authorId != null && !authorId.isBlank()) {
            hiddenContentRepository
                    .findByUserEmailIgnoreCaseAndTargetTypeIgnoreCaseAndTargetIdIgnoreCase(key, "AUTHOR",
                            authorId.trim().toLowerCase(Locale.ROOT))
                    .ifPresent(hiddenContentRepository::delete);
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("authorId", authorId);
        res.put("hidden", false);
        return res;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getHiddenContent(String userIdentity) {
        List<String> hiddenPostIds = new ArrayList<>();
        List<String> hiddenAuthorIds = new ArrayList<>();
        List<String> hiddenInvestorListingIds = new ArrayList<>();
        String key = resolveOptionalStorageKey(userIdentity);
        if (hiddenContentRepository != null && !key.isEmpty()) {
            for (HiddenContentEntity hc : hiddenContentRepository.findAllByUserEmailIgnoreCase(key)) {
                if ("POST".equalsIgnoreCase(hc.getTargetType())) {
                    hiddenPostIds.add(hc.getTargetId());
                } else if ("AUTHOR".equalsIgnoreCase(hc.getTargetType())) {
                    hiddenAuthorIds.add(hc.getTargetId());
                } else if ("INVESTOR_LISTING".equalsIgnoreCase(hc.getTargetType())) {
                    hiddenInvestorListingIds.add(hc.getTargetId());
                }
            }
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("hiddenPostIds", hiddenPostIds);
        res.put("hiddenAuthorIds", hiddenAuthorIds);
        res.put("hiddenInvestorListingIds", hiddenInvestorListingIds);
        return res;
    }

    // ==================== COMMENTS & REPLIES ====================

    @Transactional
    public Map<String, Object> commentOnPost(String id, String text, String userEmail) {
        return commentOnPost(id, text, userEmail, null);
    }

    @Transactional
    public Map<String, Object> commentOnPost(String id, String text, String userEmail, String userName) {
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
        ProfileEntity authorProfile = findCurrentProfileSafely(identity);

        post.addComment(
                resolveCommentAuthorName(authorProfile, identity, userName),
                resolveCommentAuthorAvatar(authorProfile),
                identity,
                text);
        postRepository.save(post);

        String postAuthorId = post.getAuthorId() != null ? post.getAuthorId().trim() : "";
        ProfileEntity postAuthorProfile = (!postAuthorId.isBlank() && profileService != null)
                ? findCurrentProfileSafely(postAuthorId)
                : null;
        boolean commenterNotificationsEnabled = isPostingAndCommentingAllowed(identity, authorProfile);
        boolean postAuthorNotificationsEnabled = isPostingAndCommentingAllowed(postAuthorId, postAuthorProfile);

        if (notificationRepository != null && commenterNotificationsEnabled && postAuthorNotificationsEnabled) {
            String commenterName = resolveCommentAuthorName(authorProfile, identity, userName);
            String commentSnippet = text.length() > 50 ? text.substring(0, 50) + "…" : text;
            Set<String> commenterIdentities = resolveTargetUserIdentities(identity, authorProfile);
            Set<String> postAuthorIdentities = resolveTargetUserIdentities(postAuthorId, postAuthorProfile);

            String authorDisplayName = (post.getAuthorName() != null && !post.getAuthorName().isBlank())
                    ? post.getAuthorName()
                    : (postAuthorProfile != null ? resolveCommentAuthorName(postAuthorProfile) : "Author");

            boolean commenterIsAuthor = postAuthorIdentities.stream().anyMatch(commenterIdentities::contains)
                    || commenterIdentities.contains(postAuthorId.toLowerCase(Locale.ROOT));

            Set<String> alreadyNotified = new HashSet<>(commenterIdentities);

            // 1. Post Author Notification (when commenter is not the author and connected)
            if (!postAuthorId.isBlank() && !commenterIsAuthor) {
                if (isUserConnectedWith(identity, authorProfile, postAuthorId)) {
                    if (isPostingAndCommentingAllowed(postAuthorId, postAuthorProfile)) {
                        notificationRepository.save(new NotificationEntity(
                                postAuthorId,
                                "New Comment on Your Post",
                                commenterName + " commented: \"" + commentSnippet + "\"",
                                "comment"));
                    }
                }
                alreadyNotified.addAll(postAuthorIdentities);
            }

            // 2. Remaining Connections of Post Author & Mutual Connections
            Set<String> authorConnections = findConnectedUserIds(postAuthorId, postAuthorProfile);
            for (String connId : authorConnections) {
                if (connId == null || connId.isBlank())
                    continue;
                String normalizedConn = connId.trim().toLowerCase(Locale.ROOT);
                if (alreadyNotified.contains(normalizedConn))
                    continue;

                Set<String> connIdentities = resolveTargetUserIdentities(connId, null);
                boolean isExcluded = connIdentities.stream().anyMatch(alreadyNotified::contains);
                if (isExcluded)
                    continue;

                if (isPostingAndCommentingAllowed(connId, null)) {
                    String msg = commenterIsAuthor
                            ? authorDisplayName + " commented on their post: \"" + commentSnippet + "\""
                            : commenterName + " commented on " + authorDisplayName + "'s post: \"" + commentSnippet
                                    + "\"";
                    notificationRepository.save(new NotificationEntity(
                            connId,
                            "New Comment on " + authorDisplayName + "'s Post",
                            msg,
                            "comment"));
                    alreadyNotified.add(normalizedConn);
                    alreadyNotified.addAll(connIdentities);
                }
            }
        }

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
        return replyToComment(postId, commentId, text, userIdentity, null);
    }

    @Transactional
    public Map<String, Object> replyToComment(String postId, String commentId, String text, String userIdentity, String userName) {
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
        ProfileEntity authorProfile = findCurrentProfileSafely(identity);
        if (!post.addReply(
                commentId,
                resolveCommentAuthorName(authorProfile, identity, userName),
                resolveCommentAuthorAvatar(authorProfile),
                identity,
                text.trim())) {
            throw new ResourceNotFoundException("Comment", "id", commentId);
        }

        PostEntity saved = postRepository.save(post);

        String postAuthorId = post.getAuthorId() != null ? post.getAuthorId().trim() : "";
        ProfileEntity postAuthorProfile = (!postAuthorId.isBlank() && profileService != null)
                ? findCurrentProfileSafely(postAuthorId)
                : null;
        boolean replierNotificationsEnabled = isPostingAndCommentingAllowed(identity, authorProfile);
        boolean postAuthorNotificationsEnabled = isPostingAndCommentingAllowed(postAuthorId, postAuthorProfile);

        if (notificationRepository != null && replierNotificationsEnabled && postAuthorNotificationsEnabled) {
            String replierName = resolveCommentAuthorName(authorProfile, identity, userName);
            String replySnippet = text.trim().length() > 50 ? text.trim().substring(0, 50) + "…" : text.trim();
            Set<String> replierIdentities = resolveTargetUserIdentities(identity, authorProfile);
            Set<String> postAuthorIdentities = resolveTargetUserIdentities(postAuthorId, postAuthorProfile);

            String authorDisplayName = (post.getAuthorName() != null && !post.getAuthorName().isBlank())
                    ? post.getAuthorName()
                    : (postAuthorProfile != null ? resolveCommentAuthorName(postAuthorProfile) : "Author");

            String parentCommentAuthorId = "";
            for (Map<String, Object> c : post.getComments()) {
                if (Objects.equals(String.valueOf(c.get("id")), commentId)) {
                    if (c.get("authorId") != null) {
                        parentCommentAuthorId = String.valueOf(c.get("authorId")).trim();
                    }
                    break;
                }
            }

            Set<String> parentCommentAuthorIdentities = !parentCommentAuthorId.isBlank()
                    ? resolveTargetUserIdentities(parentCommentAuthorId, null)
                    : Set.of();

            Set<String> alreadyNotified = new HashSet<>(replierIdentities);

            // 1. Notify parent comment author (if not replier)
            if (!parentCommentAuthorId.isBlank()
                    && !parentCommentAuthorIdentities.stream().anyMatch(alreadyNotified::contains)) {
                if (isPostingAndCommentingAllowed(parentCommentAuthorId, null)) {
                    notificationRepository.save(new NotificationEntity(
                            parentCommentAuthorId,
                            "New Reply to Your Comment",
                            replierName + " replied: \"" + replySnippet + "\"",
                            "comment"));
                }
                alreadyNotified.addAll(parentCommentAuthorIdentities);
            }

            // 2. Notify post author (if not replier and not parent comment author)
            if (!postAuthorId.isBlank() && !postAuthorIdentities.stream().anyMatch(alreadyNotified::contains)) {
                if (isUserConnectedWith(identity, authorProfile, postAuthorId)) {
                    if (isPostingAndCommentingAllowed(postAuthorId, postAuthorProfile)) {
                        notificationRepository.save(new NotificationEntity(
                                postAuthorId,
                                "New Reply on Your Post",
                                replierName + " replied on your post: \"" + replySnippet + "\"",
                                "comment"));
                    }
                }
                alreadyNotified.addAll(postAuthorIdentities);
            }

            // 3. Remaining Connections & Mutual Connections of Post Author
            Set<String> authorConnections = findConnectedUserIds(postAuthorId, postAuthorProfile);
            for (String connId : authorConnections) {
                if (connId == null || connId.isBlank())
                    continue;
                String normalizedConn = connId.trim().toLowerCase(Locale.ROOT);
                if (alreadyNotified.contains(normalizedConn))
                    continue;

                Set<String> connIdentities = resolveTargetUserIdentities(connId, null);
                boolean isExcluded = connIdentities.stream().anyMatch(alreadyNotified::contains);
                if (isExcluded)
                    continue;

                if (isPostingAndCommentingAllowed(connId, null)) {
                    notificationRepository.save(new NotificationEntity(
                            connId,
                            "New Comment on " + authorDisplayName + "'s Post",
                            replierName + " replied on " + authorDisplayName + "'s post: \"" + replySnippet + "\"",
                            "comment"));
                    alreadyNotified.add(normalizedConn);
                    alreadyNotified.addAll(connIdentities);
                }
            }
        }

        return toUserAwareDto(saved, identity);
    }

    @Transactional
    public Map<String, Object> deleteCommentReply(
            String postId,
            String commentId,
            String replyId,
            String userIdentity) {
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

    // ==================== OPPORTUNITY APPLICATIONS ====================

    @Transactional
    public Map<String, Object> applyToOpportunity(String id, String applicantIdentity) {
        return applyToOpportunity(id, applicantIdentity, null);
    }

    @Transactional
    public Map<String, Object> applyToOpportunity(String id, String applicantIdentity, Map<String, Object> details) {
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

        OpportunityApplicationEntity application = new OpportunityApplicationEntity(numericId, identity);
        if (details != null) {
            String name = firstNonBlank(details, "applicantName", "applicationName", "name");
            String exp = firstNonBlank(details, "experience", "applicationExperience");
            String note = firstNonBlank(details, "note", "pitchNote", "message");
            if (!name.isEmpty())
                application.setApplicantName(name);
            if (!exp.isEmpty())
                application.setExperience(exp);
            if (!note.isEmpty())
                application.setNote(note);
        }
        opportunityApplicationRepository.save(application);
        post.setApplicationsCount(post.getApplicationsCount() + 1);
        postRepository.save(post);

        Map<String, Object> opportunityDetails = Optional.ofNullable(post.getOpportunityDetails())
                .orElseGet(Collections::emptyMap);
        String title = firstNonBlank(
                Map.of(
                        "title", post.getTitle() != null ? post.getTitle() : "",
                        "projectName", opportunityDetails.get("projectName") != null
                                ? String.valueOf(opportunityDetails.get("projectName"))
                                : "",
                        "projectCampaignName", opportunityDetails.get("projectCampaignName") != null
                                ? String.valueOf(opportunityDetails.get("projectCampaignName"))
                                : ""
                ),
                "title",
                "projectName",
                "projectCampaignName"
        );
        if (title.isBlank()) title = "casting call";

        ProfileEntity applicantProfile = findCurrentProfileSafely(identity);
        String applicantName = firstNonBlank(
                Map.of(
                        "applicationName", application.getApplicantName() != null ? application.getApplicantName() : "",
                        "profileName", applicantProfile != null && applicantProfile.getName() != null
                                ? applicantProfile.getName()
                                : ""
                ),
                "applicationName",
                "profileName"
        );
        if (applicantName.isBlank()) applicantName = "A talent";

        ProfileEntity posterProfile = findCurrentProfileSafely(post.getAuthorId());
        String posterName = post.getAuthorName() != null && !post.getAuthorName().isBlank()
                ? post.getAuthorName().trim()
                : posterProfile != null && posterProfile.getName() != null && !posterProfile.getName().isBlank()
                        ? posterProfile.getName().trim()
                        : "the casting team";
        String posterNotificationRecipient = posterProfile != null
                && posterProfile.getEmail() != null
                && !posterProfile.getEmail().isBlank()
                        ? posterProfile.getEmail().trim()
                        : post.getAuthorId();

        if (notificationRepository != null) {
            notificationRepository.save(new NotificationEntity(
                    identity,
                    "Application Sent",
                    "Your application for \"" + title + "\" was sent to " + posterName + ".",
                    "opportunity_application",
                    post.getAuthorId()));

            if (posterNotificationRecipient != null
                    && !posterNotificationRecipient.isBlank()
                    && !isAuthorOfPost(post, identity)
                    && isPostingAndCommentingAllowed(posterNotificationRecipient, posterProfile)) {
                notificationRepository.save(new NotificationEntity(
                        posterNotificationRecipient,
                        "New Application Received",
                        applicantName + " applied to your casting call \"" + title + "\".",
                        "opportunity_application",
                        identity));
            }
        }

        return applicationResponse(id, post, true, "Application submitted successfully.");
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getOpportunityApplications(String postId, String userIdentity) {
        PostEntity post = findOwnedOpportunity(postId, userIdentity);
        List<Map<String, Object>> applications = new ArrayList<>();
        for (OpportunityApplicationEntity application :
                opportunityApplicationRepository.findAllByPostIdOrderByAppliedAtDesc(post.getId())) {
            String applicantIdentity = application.getApplicantIdentity();
            ProfileEntity profile = profileService != null
                    ? profileService.findCurrentProfile(applicantIdentity)
                    : null;
            Map<String, Object> applicationDto = new LinkedHashMap<>();
            applicationDto.put("id", String.valueOf(application.getId()));
            applicationDto.put("applicantId", profile != null && profile.getUserId() != null
                    ? profile.getUserId()
                    : applicantIdentity);
            applicationDto.put("applicantName", firstNonBlank(
                    Map.of(
                            "application", application.getApplicantName() != null ? application.getApplicantName() : "",
                            "profile", profile != null && profile.getName() != null ? profile.getName() : ""
                    ),
                    "application",
                    "profile"
            ));
            applicationDto.put("profileImageUrl", profile != null ? profile.getProfileImageUrl() : "");
            applicationDto.put("headline", profile != null
                    ? firstNonBlank(Map.of(
                            "headline", profile.getHeadline() != null ? profile.getHeadline() : "",
                            "role", profile.getRole() != null ? profile.getRole() : ""
                    ), "headline", "role")
                    : "");
            applicationDto.put("experience", application.getExperience() != null ? application.getExperience() : "");
            applicationDto.put("applicationDetails", parseJsonMap(application.getNote()));
            applicationDto.put("note", application.getNote() != null ? application.getNote() : "");
            applicationDto.put("appliedAt", application.getAppliedAt() != null
                    ? application.getAppliedAt().toString()
                    : "");
            applicationDto.put("shortlisted", application.isShortlisted());
            applications.add(applicationDto);
        }
        return applications;
    }

    @Transactional
    public Map<String, Object> updateOpportunityApplicationShortlist(
            String postId,
            String applicationId,
            boolean shortlisted,
            String userIdentity) {
        PostEntity post = findOwnedOpportunity(postId, userIdentity);
        Long numericApplicationId = parseLongId(applicationId);
        if (numericApplicationId == null) {
            throw new ResourceNotFoundException("Opportunity application", "id", applicationId);
        }
        OpportunityApplicationEntity application = opportunityApplicationRepository
                .findByIdAndPostId(numericApplicationId, post.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity application", "id", applicationId));
        application.setShortlisted(shortlisted);
        opportunityApplicationRepository.save(application);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", String.valueOf(application.getId()));
        response.put("shortlisted", application.isShortlisted());
        return response;
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
        Optional<OpportunityApplicationEntity> application = opportunityApplicationRepository
                .findByPostIdAndApplicantIdentity(numericId, identity);

        if (application.isPresent()) {
            opportunityApplicationRepository.delete(application.get());
            post.setApplicationsCount(Math.max(0, post.getApplicationsCount() - 1));
            postRepository.save(post);
        }

        return applicationResponse(id, post, false, "Application cancelled successfully.");
    }

    private PostEntity findOwnedOpportunity(String postId, String userIdentity) {
        normalizeApplicantIdentity(userIdentity);
        Long numericPostId = parseLongId(postId);
        if (numericPostId == null) {
            throw new ResourceNotFoundException("Opportunity", "id", postId);
        }
        PostEntity post = postRepository.findById(numericPostId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity", "id", postId));
        if (!"opportunity".equalsIgnoreCase(post.getType())) {
            throw new ResourceNotFoundException("Opportunity", "id", postId);
        }
        if (!isAuthorOfPost(post, userIdentity)) {
            throw new UnauthorizedException("You can only view or manage applications for casting calls you posted.");
        }
        return post;
    }

    // ==================== HELPERS ====================

    private boolean isPostVisibleToCaller(
            PostEntity post,
            Set<String> callerIdentities,
            Set<String> connectedTargetIds,
            Set<String> hiddenPostIds,
            Set<String> hiddenAuthorIds,
            Set<String> blockedIdentities) {
        if (post.getId() != null && hiddenPostIds.contains(String.valueOf(post.getId()))) {
            return false;
        }
        String authorIdNorm = normalizeCommentOwnerIdentity(post.getAuthorId());
        String authorNameNorm = normalizeCommentOwnerIdentity(post.getAuthorName());
        boolean isOwner = isPostOwner(post, callerIdentities);
        if (!isOwner) {
            if (!authorIdNorm.isEmpty()
                    && (hiddenAuthorIds.contains(authorIdNorm) || blockedIdentities.contains(authorIdNorm))) {
                return false;
            }
            if (!authorNameNorm.isEmpty()
                    && (hiddenAuthorIds.contains(authorNameNorm) || blockedIdentities.contains(authorNameNorm))) {
                return false;
            }
        }
        return isAudienceAllowed(post, callerIdentities, connectedTargetIds);
    }

    private boolean isAudienceAllowed(PostEntity post, Set<String> callerIdentities, Set<String> connectedTargetIds) {
        if (isPostOwner(post, callerIdentities)) {
            return true;
        }
        String audience = normalizeAudience(post.getAudience());
        if ("private".equals(audience)) {
            return false;
        }
        if ("connections".equals(audience)) {
            String authorIdNorm = normalizeCommentOwnerIdentity(post.getAuthorId());
            if (authorIdNorm.isEmpty() || !connectedTargetIds.contains(authorIdNorm)) {
                return false;
            }
        }
        return true;
    }

    private Set<String> resolveConnectedTargetIds(Set<String> callerIdentities) {
        Set<String> connected = new HashSet<>();
        if (relationshipRepository == null)
            return connected;
        for (String cid : callerIdentities) {
            for (UserProfileRelationshipEntity rel : relationshipRepository.findAllByOwnerUserIdAndConnectedTrue(cid)) {
                if (rel.getTargetProfileId() != null && !rel.getTargetProfileId().isBlank()) {
                    connected.add(rel.getTargetProfileId().trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        return connected;
    }

    private boolean isCurrentUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority()));
    }

    private boolean isDirectProfileMatch(ProfileEntity profile, String userIdentity) {
        if (profile == null) {
            return false;
        }
        if (userIdentity == null || userIdentity.isBlank()) {
            return true;
        }
        String norm = normalizeCommentOwnerIdentity(userIdentity);
        if ("me".equals(norm)) {
            return true;
        }
        String profId = normalizeCommentOwnerIdentity(profile.getUserId());
        String profEmail = normalizeCommentOwnerIdentity(profile.getEmail());
        if (profId.isEmpty() && profEmail.isEmpty()) {
            return true;
        }
        return (!profId.isEmpty() && norm.equals(profId))
                || (!profEmail.isEmpty() && norm.equals(profEmail));
    }

    private Set<String> resolveOptionalCallerIdentities(String userIdentity) {
        if (userIdentity == null || userIdentity.isBlank()) {
            return Set.of();
        }
        Set<String> identities = new HashSet<>();
        identities.add(normalizeCommentOwnerIdentity(userIdentity));
        try {
            ProfileEntity profile = profileService.findCurrentProfile(userIdentity);
            if (profile != null && isDirectProfileMatch(profile, userIdentity)) {
                String profId = normalizeCommentOwnerIdentity(profile.getUserId());
                String profEmail = normalizeCommentOwnerIdentity(profile.getEmail());
                identities.add(profId);
                identities.add(profEmail);
                if ("1000001".equals(profId) || "pro@truehunt.in".equals(profEmail)) {
                    identities.add("me");
                }
            }
        } catch (Exception ignored) {
        }
        identities.remove("");
        return identities;
    }

    private String resolveOptionalStorageKey(String userIdentity) {
        if (userIdentity == null || userIdentity.isBlank())
            return "";
        try {
            ProfileEntity profile = profileService.findCurrentProfile(userIdentity);
            if (profile != null && isDirectProfileMatch(profile, userIdentity)
                    && profile.getEmail() != null && !profile.getEmail().isBlank()) {
                return profile.getEmail().trim().toLowerCase(Locale.ROOT);
            }
        } catch (Exception ignored) {
        }
        return userIdentity.trim().toLowerCase(Locale.ROOT);
    }

    private String requireStorageKey(String userIdentity, String errorMessage) {
        if (userIdentity == null || userIdentity.isBlank()) {
            throw new UnauthorizedException(errorMessage);
        }
        return resolveOptionalStorageKey(userIdentity);
    }

    private String normalizeAudience(String audience) {
        if (audience == null || audience.isBlank())
            return "public";
        String norm = audience.trim().toLowerCase(Locale.ROOT);
        if ("connections".equals(norm) || "private".equals(norm)) {
            return norm;
        }
        return "public";
    }

    private Set<String> resolveCommentOwnerIdentities(String userIdentity) {
        if (userIdentity == null || userIdentity.isBlank()) {
            throw new UnauthorizedException("Sign in to delete comments.");
        }

        ProfileEntity profile = findCurrentProfileSafely(userIdentity);
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
        if (userIdentity != null && !userIdentity.isBlank())
            return userIdentity.trim();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "pro@truehunt.in";
    }

    private String resolveCommentAuthorName(ProfileEntity profile) {
        return resolveCommentAuthorName(profile, null, null);
    }

    private String resolveCommentAuthorName(ProfileEntity profile, String identity, String explicitName) {
        if (profile != null && profile.getName() != null && !profile.getName().isBlank()) {
            return profile.getName();
        }
        if (explicitName != null && !explicitName.isBlank()) {
            return explicitName.trim();
        }
        if (identity != null && !identity.isBlank() && !"me".equalsIgnoreCase(identity.trim())) {
            String clean = identity.trim();
            if (clean.contains("@")) {
                String prefix = clean.substring(0, clean.indexOf('@')).trim();
                if (!prefix.isEmpty()) {
                    return prefix;
                }
            } else {
                return clean;
            }
        }
        return "Cinema Professional";
    }

    private String resolveCommentAuthorAvatar(ProfileEntity profile) {
        return profile != null && profile.getProfileImageUrl() != null
                ? profile.getProfileImageUrl()
                : "";
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

    public boolean isAuthorOfPost(PostEntity post, String userIdentity) {
        if (post == null || userIdentity == null || userIdentity.isBlank()) {
            return false;
        }
        String normUser = normalizeCommentOwnerIdentity(userIdentity);
        String normAuthor = normalizeCommentOwnerIdentity(post.getAuthorId());
        if (normAuthor.isEmpty()) {
            return false;
        }
        if (normAuthor.equals(normUser)) {
            return true;
        }
        if ("me".equals(normUser) && ("pro@truehunt.in".equals(normAuthor) || "1000001".equals(normAuthor))) {
            return true;
        }
        if ("me".equals(normAuthor) && ("pro@truehunt.in".equals(normUser) || "1000001".equals(normUser))) {
            return true;
        }
        Set<String> callerIdentities = resolveOptionalCallerIdentities(userIdentity);
        if (callerIdentities.contains(normAuthor)) {
            return true;
        }
        if (profileService != null) {
            try {
                ProfileEntity authorProfile = profileService.findCurrentProfile(normAuthor);
                if (authorProfile != null) {
                    String authId = normalizeCommentOwnerIdentity(authorProfile.getUserId());
                    String authEmail = normalizeCommentOwnerIdentity(authorProfile.getEmail());
                    if (callerIdentities.contains(authId) || callerIdentities.contains(authEmail)
                            || normUser.equals(authId) || normUser.equals(authEmail)) {
                        return true;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private Map<String, Object> toUserAwareDto(PostEntity post, String userIdentity) {
        Map<String, Object> dto = post.toDto(userIdentity);
        if ("opportunity".equalsIgnoreCase(post.getType())) {
            String identity = userIdentity == null ? "" : userIdentity.trim().toLowerCase(Locale.ROOT);
            dto.put("applied", !identity.isEmpty()
                    && opportunityApplicationRepository.existsByPostIdAndApplicantIdentity(post.getId(), identity));
        }
        if (savedPostRepository != null && post.getId() != null) {
            String key = resolveOptionalStorageKey(userIdentity);
            dto.put("saved",
                    !key.isEmpty() && savedPostRepository.existsByUserEmailIgnoreCaseAndPostId(key, post.getId()));
        }
        boolean isAuthor = isAuthorOfPost(post, userIdentity);
        dto.put("isAuthor", isAuthor);
        if (settingsService != null) {
            boolean likeCountEnabled = settingsService.isLikeCountEnabledForAuthor(post.getAuthorId());
            boolean shareCountEnabled = settingsService.isShareCountEnabledForAuthor(post.getAuthorId());

            if (isAuthor) {
                // Original user ALWAYS sees the like and share counts
                dto.put("likesCount", post.getLikesCount());
                dto.put("sharesCount", post.getSharesCount());
                dto.put("showLikesCount", true);
                dto.put("showSharesCount", true);
                dto.put("likeCountDisabled", false);
                dto.put("shareCountDisabled", false);
            } else {
                // Other users: if author stopped the count, do NOT show to other users
                if (!likeCountEnabled) {
                    dto.put("likesCount", 0);
                    dto.put("showLikesCount", false);
                    dto.put("likeCountDisabled", true);
                } else {
                    dto.put("showLikesCount", true);
                    dto.put("likeCountDisabled", false);
                }
                if (!shareCountEnabled) {
                    dto.put("sharesCount", 0);
                    dto.put("showSharesCount", false);
                    dto.put("shareCountDisabled", true);
                } else {
                    dto.put("showSharesCount", true);
                    dto.put("shareCountDisabled", false);
                }
            }
        } else {
            dto.put("showLikesCount", true);
            dto.put("showSharesCount", true);
            dto.put("likeCountDisabled", false);
            dto.put("shareCountDisabled", false);
        }
        return dto;
    }

    private Map<String, Object> applicationResponse(
            String id,
            PostEntity post,
            boolean applied,
            String message) {
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

    private String writeJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Map<String, Object> parseJsonMap(String json) {
        if (json == null || json.isBlank())
            return new HashMap<>();
        try {
            return MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private String stringValue(Map<String, Object> map, String key) {
        if (map == null || map.get(key) == null)
            return "";
        return String.valueOf(map.get(key)).trim();
    }

    private String firstNonBlank(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            String val = stringValue(map, key);
            if (!val.isEmpty())
                return val;
        }
        return "";
    }

    private Set<String> findConnectedUserIds(String authorIdentifier, ProfileEntity authorProfile) {
        if (relationshipRepository == null) {
            return Collections.emptySet();
        }
        Set<String> authorIds = resolveTargetUserIdentities(authorIdentifier, authorProfile);
        Set<String> connectedRecipients = new LinkedHashSet<>();
        for (UserProfileRelationshipEntity rel : relationshipRepository.findAll()) {
            if (rel == null || (!rel.isConnected() && !"ACCEPTED".equalsIgnoreCase(rel.getStatus()))) {
                continue;
            }
            String owner = rel.getOwnerUserId() != null ? rel.getOwnerUserId().trim().toLowerCase(Locale.ROOT) : "";
            String target = rel.getTargetProfileId() != null ? rel.getTargetProfileId().trim().toLowerCase(Locale.ROOT)
                    : "";
            if (authorIds.contains(owner) && !authorIds.contains(target) && !target.isBlank()) {
                connectedRecipients.add(rel.getTargetProfileId().trim());
            } else if (authorIds.contains(target) && !authorIds.contains(owner) && !owner.isBlank()) {
                connectedRecipients.add(rel.getOwnerUserId().trim());
            }
        }
        return connectedRecipients;
    }

    private boolean isUserConnectedWith(String userA, ProfileEntity profileA, String userB) {
        if (relationshipRepository == null || userB == null || userB.isBlank()) {
            return false;
        }
        Set<String> aIds = resolveTargetUserIdentities(userA, profileA);
        Set<String> bIds = resolveTargetUserIdentities(userB, null);
        for (UserProfileRelationshipEntity rel : relationshipRepository.findAll()) {
            if (rel == null || (!rel.isConnected() && !"ACCEPTED".equalsIgnoreCase(rel.getStatus()))) {
                continue;
            }
            String owner = rel.getOwnerUserId() != null ? rel.getOwnerUserId().trim().toLowerCase(Locale.ROOT) : "";
            String target = rel.getTargetProfileId() != null ? rel.getTargetProfileId().trim().toLowerCase(Locale.ROOT)
                    : "";
            boolean pairMatch = (aIds.contains(owner) && bIds.contains(target))
                    || (aIds.contains(target) && bIds.contains(owner));
            if (pairMatch) {
                return true;
            }
        }
        return false;
    }

    private Set<String> resolveAuthorIdentities(String identity, ProfileEntity profile) {
        return resolveTargetUserIdentities(identity, profile);
    }

    private Set<String> resolveTargetUserIdentities(String identifier, ProfileEntity profile) {
        Set<String> set = new HashSet<>();
        if (identifier != null && !identifier.isBlank()) {
            set.add(identifier.trim().toLowerCase(Locale.ROOT));
        }
        ProfileEntity p = profile;
        if (p == null && identifier != null && !identifier.isBlank() && profileService != null) {
            try {
                p = profileService.findCurrentProfile(identifier);
            } catch (Exception ignored) {
            }
        }
        if (p != null) {
            if (p.getUserId() != null && !p.getUserId().isBlank()) {
                set.add(p.getUserId().trim().toLowerCase(Locale.ROOT));
            }
            if (p.getEmail() != null && !p.getEmail().isBlank()) {
                set.add(p.getEmail().trim().toLowerCase(Locale.ROOT));
            }
        }
        return set;
    }

    private boolean isPostingAndCommentingAllowed(String userIdentifier, ProfileEntity profile) {
        if (settingsService == null) {
            return true;
        }
        if (userIdentifier != null && !userIdentifier.isBlank()) {
            if (!settingsService.isPostingAndCommentingEnabled(userIdentifier)) {
                return false;
            }
        }
        Set<String> identities = resolveTargetUserIdentities(userIdentifier, profile);
        for (String id : identities) {
            if (id != null && !id.isBlank()) {
                if (!settingsService.isPostingAndCommentingEnabled(id)) {
                    return false;
                }
            }
        }
        return true;
    }
}
