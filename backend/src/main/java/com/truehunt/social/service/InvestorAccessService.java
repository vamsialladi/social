package com.truehunt.social.service;

import com.truehunt.social.dto.InvestorAccessRequestDto;
import com.truehunt.social.dto.CreatePostRequest;
import com.truehunt.social.dto.CreateConversationRequest;
import com.truehunt.social.exception.BadRequestException;
import com.truehunt.social.exception.ResourceNotFoundException;
import com.truehunt.social.exception.UnauthorizedException;
import com.truehunt.social.model.HiddenContentEntity;
import com.truehunt.social.model.InvestorAccessRequestEntity;
import com.truehunt.social.model.NotificationEntity;
import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.repository.HiddenContentRepository;
import com.truehunt.social.repository.InvestorAccessRequestRepository;
import com.truehunt.social.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class InvestorAccessService {

    private final InvestorAccessRequestRepository requestRepository;
    private final UserAccountRepository userAccountRepository;
    private final PostService postService;
    private final ConversationService conversationService;
    private final NotificationService notificationService;
    private final HiddenContentRepository hiddenContentRepository;

    public InvestorAccessService(InvestorAccessRequestRepository requestRepository,
                                 UserAccountRepository userAccountRepository,
                                 PostService postService,
                                 ConversationService conversationService,
                                 NotificationService notificationService) {
        this(requestRepository, userAccountRepository, postService, conversationService, notificationService, null);
    }

    @Autowired
    public InvestorAccessService(InvestorAccessRequestRepository requestRepository,
                                 UserAccountRepository userAccountRepository,
                                 PostService postService,
                                 ConversationService conversationService,
                                 NotificationService notificationService,
                                 HiddenContentRepository hiddenContentRepository) {
        this.requestRepository = requestRepository;
        this.userAccountRepository = userAccountRepository;
        this.postService = postService;
        this.conversationService = conversationService;
        this.notificationService = notificationService;
        this.hiddenContentRepository = hiddenContentRepository;
    }

    @Transactional
    public Map<String, Object> submit(String email, InvestorAccessRequestDto request) {
        UserAccountEntity account = userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "email", email));

        InvestorAccessRequestEntity latest = requestRepository
                .findFirstByApplicantEmailIgnoreCaseOrderByCreatedAtDesc(email)
                .orElse(null);
        if (latest != null && "PUBLISHED".equals(latest.getStatus())) {
            return toMap(latest);
        }

        InvestorAccessRequestEntity entity = latest != null && !"REJECTED".equals(latest.getStatus())
                ? latest
                : new InvestorAccessRequestEntity();
        entity.setApplicantAccountId(account.getId());
        entity.setApplicantEmail(account.getEmail());
        entity.setUserId(account.getPublicUserId() == null ? "" : account.getPublicUserId());
        entity.setName(request.getName().trim());
        entity.setMobileNumber(request.getMobileNumber() == null ? "" : request.getMobileNumber().trim());
        entity.setBudget(request.getBudget().trim());
        entity.setRole(request.getRole().trim());
        entity.setDescription(request.getDescription().trim());
        entity.setStatus("PUBLISHED");
        InvestorAccessRequestEntity published = requestRepository.save(entity);

        CreatePostRequest feedPostRequest = new CreatePostRequest();
        feedPostRequest.setType("post");
        feedPostRequest.setInvestorProfileId(published.getId());
        feedPostRequest.setTitle(published.getRole() + " profile");
        String content = "Role: " + published.getRole();
        if (!budgetMatchesMobileNumber(published)) {
            content += "\nInvestment budget: " + formatBudgetForDisplay(published.getBudget());
        }
        feedPostRequest.setContent(content + "\n\n" + published.getDescription());
        Map<String, Object> feedPost = postService.createPost(feedPostRequest, email);
        published.setFeedPostId(Long.parseLong(String.valueOf(feedPost.get("id"))));
        published = requestRepository.save(published);
        Map<String, Object> result = toMap(published);
        result.put("feedPost", feedPost);
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStatus(String email) {
        return requestRepository.findFirstByApplicantEmailIgnoreCaseOrderByCreatedAtDesc(email)
                .map(this::toMap)
                .orElseGet(() -> Map.of("status", "NONE"));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllRequests() {
        return requestRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toMap).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPublishedListings(String email) {
        Set<String> hiddenListingIds = new HashSet<>();
        if (hiddenContentRepository != null && email != null && !email.isBlank()) {
            String key = email.trim().toLowerCase(Locale.ROOT);
            for (HiddenContentEntity hc : hiddenContentRepository
                    .findAllByUserEmailIgnoreCaseAndTargetTypeIgnoreCase(key, "INVESTOR_LISTING")) {
                if (hc.getTargetId() != null) {
                    hiddenListingIds.add(hc.getTargetId().trim());
                }
            }
        }
        return requestRepository.findAllByStatusOrderByCreatedAtDesc("PUBLISHED")
                .stream()
                .filter(listing -> listing.getId() == null || !hiddenListingIds.contains(String.valueOf(listing.getId())))
                .map(listing -> toPublicListingMap(listing, email))
                .toList();
    }

    @Transactional
    public Map<String, Object> hideListing(String rawListingId, String email) {
        if (email == null || email.isBlank()) {
            throw new UnauthorizedException("Sign in to update investor feed preferences.");
        }
        String key = email.trim().toLowerCase(Locale.ROOT);
        Long listingId = parseListingId(rawListingId);
        String targetId = listingId != null ? String.valueOf(listingId) : rawListingId.trim();
        if (hiddenContentRepository != null && !targetId.isEmpty()) {
            hiddenContentRepository
                    .findByUserEmailIgnoreCaseAndTargetTypeIgnoreCaseAndTargetIdIgnoreCase(key, "INVESTOR_LISTING", targetId)
                    .orElseGet(() -> hiddenContentRepository.save(new HiddenContentEntity(key, "INVESTOR_LISTING", targetId)));
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id", targetId);
        res.put("hidden", true);
        return res;
    }

    @Transactional
    public Map<String, Object> unhideListing(String rawListingId, String email) {
        if (email == null || email.isBlank()) {
            throw new UnauthorizedException("Sign in to update investor feed preferences.");
        }
        String key = email.trim().toLowerCase(Locale.ROOT);
        Long listingId = parseListingId(rawListingId);
        String targetId = listingId != null ? String.valueOf(listingId) : rawListingId.trim();
        if (hiddenContentRepository != null && !targetId.isEmpty()) {
            hiddenContentRepository
                    .findByUserEmailIgnoreCaseAndTargetTypeIgnoreCaseAndTargetIdIgnoreCase(key, "INVESTOR_LISTING", targetId)
                    .ifPresent(hiddenContentRepository::delete);
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id", targetId);
        res.put("hidden", false);
        return res;
    }

    @Transactional
    public Map<String, Object> sendInvestorInterest(String rawListingId, String senderEmail) {
        if (senderEmail == null || senderEmail.isBlank()) {
            throw new UnauthorizedException("Sign in to contact this investor.");
        }
        Long listingId = parseListingId(rawListingId);
        if (listingId == null) {
            throw new ResourceNotFoundException("Investor profile", "id", rawListingId);
        }
        return sendInvestorInterest(listingId, senderEmail);
    }

    @Transactional
    public Map<String, Object> sendInvestorInterest(Long listingId, String senderEmail) {
        if (senderEmail == null || senderEmail.isBlank()) {
            throw new UnauthorizedException("Sign in to contact this investor.");
        }

        InvestorAccessRequestEntity listing = requestRepository.findById(listingId)
                .filter(item -> "PUBLISHED".equals(item.getStatus()))
                .orElse(null);
        if (listing == null) {
            listing = requestRepository.findFirstByFeedPostId(listingId)
                    .filter(item -> "PUBLISHED".equals(item.getStatus()))
                    .orElse(null);
        }
        if (listing == null) {
            throw new ResourceNotFoundException("Investor profile", "id", listingId);
        }
        if (listing.getApplicantEmail() == null
                || listing.getApplicantEmail().equalsIgnoreCase(senderEmail.trim())) {
            throw new BadRequestException("You cannot send an interest message to your own investor profile.");
        }

        final String ownerEmail = listing.getApplicantEmail();
        UserAccountEntity sender = userAccountRepository.findByEmail(senderEmail.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("Account", "email", senderEmail));
        UserAccountEntity owner = userAccountRepository.findByEmail(
                        ownerEmail.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("Account", "email", ownerEmail));
        String senderName = sender.getName() == null || sender.getName().isBlank()
                ? sender.getEmail().split("@")[0]
                : sender.getName().trim();
        String message = senderName + " is interested in investing in your project.";

        CreateConversationRequest conversationRequest = new CreateConversationRequest();
        conversationRequest.setRecipient(
                owner.getPublicUserId() == null || owner.getPublicUserId().isBlank()
                        ? owner.getEmail()
                        : owner.getPublicUserId()
        );
        conversationRequest.setMessageText(message);
        Map<String, Object> conversation = conversationService.createConversation(conversationRequest, sender.getEmail());
        NotificationEntity notification = notificationService.createNotification(
                owner.getEmail(),
                "Investor Interest",
                message,
                "investor-interest"
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("conversation", conversation);
        result.put("notification", notification.toDto());
        result.put("message", message);
        return result;
    }

    @Transactional
    public boolean deletePublishedListingForDeletedPost(String postId, String email) {
        if (postId == null || !postId.matches("\\d+") || email == null || email.isBlank()) {
            return false;
        }

        InvestorAccessRequestEntity listing = requestRepository
                .findFirstByFeedPostIdAndApplicantEmailIgnoreCase(Long.valueOf(postId), email)
                .orElse(null);
        if (listing == null || !"PUBLISHED".equals(listing.getStatus())) {
            return false;
        }

        requestRepository.delete(listing);
        return true;
    }

    @Transactional
    public Map<String, Object> deletePublishedListing(String rawListingId, String email) {
        if (email == null || email.isBlank()) {
            throw new UnauthorizedException("Sign in to delete this investor profile.");
        }
        Long listingId = parseListingId(rawListingId);
        if (listingId == null) {
            throw new ResourceNotFoundException("Investor profile", "id", rawListingId);
        }
        return deletePublishedListing(listingId, email);
    }

    @Transactional
    public Map<String, Object> deletePublishedListing(Long listingId, String email) {
        if (email == null || email.isBlank()) {
            throw new UnauthorizedException("Sign in to delete this investor profile.");
        }
        InvestorAccessRequestEntity listing = requestRepository.findById(listingId).orElse(null);
        if (listing == null) {
            listing = requestRepository.findFirstByFeedPostIdAndApplicantEmailIgnoreCase(listingId, email).orElse(null);
        }
        if (listing == null) {
            throw new ResourceNotFoundException("Investor profile", "id", listingId);
        }
        if (listing.getApplicantEmail() == null || !listing.getApplicantEmail().equalsIgnoreCase(email)) {
            throw new UnauthorizedException("You can only delete investor profiles that you created.");
        }

        Long feedPostId = listing.getFeedPostId();
        if (feedPostId != null) {
            postService.deletePostIfPresent(String.valueOf(feedPostId), email);
        }
        requestRepository.delete(listing);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Investor profile deleted successfully");
        result.put("id", listing.getId());
        if (feedPostId != null) {
            result.put("feedPostId", feedPostId);
        }
        return result;
    }

    @Transactional
    public Map<String, Object> review(Long requestId, String decision, String adminEmail) {
        InvestorAccessRequestEntity request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Investor access request", "id", requestId));
        String currentStatus = request.getStatus() == null ? "" : request.getStatus().trim().toUpperCase(Locale.ROOT);
        String normalizedDecision = decision == null ? "" : decision.trim().toUpperCase(Locale.ROOT);

        boolean allowed = "PENDING".equals(currentStatus)
                || ("PUBLISHED".equals(currentStatus) && "REJECTED".equals(normalizedDecision))
                || ("REJECTED".equals(currentStatus) && "APPROVED".equals(normalizedDecision));
        if (!allowed) {
            throw new BadRequestException("This investor access request has already been reviewed.");
        }

        if ("REJECTED".equals(normalizedDecision) && request.getFeedPostId() != null && request.getApplicantEmail() != null) {
            try {
                postService.deletePostIfPresent(String.valueOf(request.getFeedPostId()), request.getApplicantEmail());
                request.setFeedPostId(null);
            } catch (Exception ignored) {}
        }

        if ("REJECTED".equals(currentStatus) && "APPROVED".equals(normalizedDecision)) {
            request.setStatus("PUBLISHED");
        } else {
            request.setStatus(normalizedDecision);
        }
        request.setReviewedBy(adminEmail);
        request.setReviewedAt(LocalDateTime.now());
        return toMap(requestRepository.save(request));
    }

    private Map<String, Object> toMap(InvestorAccessRequestEntity request) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", request.getId());
        result.put("userId", request.getUserId());
        result.put("name", request.getName());
        result.put("email", request.getApplicantEmail());
        result.put("mobileNumber", request.getMobileNumber());
        result.put("budget", request.getBudget());
        result.put("description", request.getDescription());
        result.put("role", request.getRole());
        result.put("status", request.getStatus());
        result.put("createdAt", request.getCreatedAt());
        result.put("reviewedAt", request.getReviewedAt());
        result.put("reviewedBy", request.getReviewedBy());
        return result;
    }

    private Map<String, Object> toPublicListingMap(InvestorAccessRequestEntity request, String email) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", request.getId());
        result.put("name", request.getName());
        result.put("role", request.getRole());
        result.put("description", request.getDescription());
        if (!budgetMatchesMobileNumber(request)) {
            result.put("budget", formatBudgetForDisplay(request.getBudget()));
        }
        result.put("createdAt", request.getCreatedAt());
        boolean isOwner = request.getApplicantEmail() != null
                && request.getApplicantEmail().equalsIgnoreCase(email);
        result.put("isMine", isOwner);
        if (isOwner) {
            result.put("feedPostId", request.getFeedPostId());
        }
        return result;
    }

    private boolean budgetMatchesMobileNumber(InvestorAccessRequestEntity request) {
        String budgetDigits = request.getBudget() == null
                ? ""
                : request.getBudget().replaceAll("\\D", "");
        String mobileDigits = request.getMobileNumber() == null
                ? ""
                : request.getMobileNumber().replaceAll("\\D", "");
        return mobileDigits.length() >= 7
                && budgetDigits.length() >= 7
                && (budgetDigits.equals(mobileDigits)
                || mobileDigits.endsWith(budgetDigits)
                || budgetDigits.endsWith(mobileDigits));
    }

    private String formatBudgetForDisplay(String value) {
        String budget = value == null ? "" : value.trim();
        if (budget.isEmpty() || budget.startsWith("₹")) return budget;
        return "₹" + budget.replaceFirst("^(?i:INR\\b|Rs\\.?|[$€£¥])\\s*", "");
    }

    private Long parseListingId(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String clean = raw.trim();
        if (clean.toLowerCase(Locale.ROOT).startsWith("social-")) {
            clean = clean.substring("social-".length());
        } else if (clean.toLowerCase(Locale.ROOT).startsWith("inv-")) {
            clean = clean.substring("inv-".length());
        } else if (clean.toLowerCase(Locale.ROOT).startsWith("listing-")) {
            clean = clean.substring("listing-".length());
        }
        try {
            return Long.parseLong(clean);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
