package com.truehunt.social.service;

import com.truehunt.social.dto.InvestorAccessRequestDto;
import com.truehunt.social.dto.CreatePostRequest;
import com.truehunt.social.exception.BadRequestException;
import com.truehunt.social.exception.ResourceNotFoundException;
import com.truehunt.social.exception.UnauthorizedException;
import com.truehunt.social.model.InvestorAccessRequestEntity;
import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.repository.InvestorAccessRequestRepository;
import com.truehunt.social.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class InvestorAccessService {

    private final InvestorAccessRequestRepository requestRepository;
    private final UserAccountRepository userAccountRepository;
    private final PostService postService;

    public InvestorAccessService(InvestorAccessRequestRepository requestRepository,
                                 UserAccountRepository userAccountRepository,
                                 PostService postService) {
        this.requestRepository = requestRepository;
        this.userAccountRepository = userAccountRepository;
        this.postService = postService;
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
            content += "\nInvestment budget: " + published.getBudget();
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
        return requestRepository.findAllByStatusOrderByCreatedAtDesc("PUBLISHED")
                .stream()
                .map(listing -> toPublicListingMap(listing, email))
                .toList();
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
    public Map<String, Object> deletePublishedListing(Long listingId, String email) {
        if (email == null || email.isBlank()) {
            throw new UnauthorizedException("Sign in to delete this investor profile.");
        }
        InvestorAccessRequestEntity listing = requestRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Investor profile", "id", listingId));
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
        result.put("id", listingId);
        if (feedPostId != null) {
            result.put("feedPostId", feedPostId);
        }
        return result;
    }

    @Transactional
    public Map<String, Object> review(Long requestId, String decision, String adminEmail) {
        InvestorAccessRequestEntity request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Investor access request", "id", requestId));
        if (!"PENDING".equals(request.getStatus())) {
            throw new BadRequestException("This investor access request has already been reviewed.");
        }

        request.setStatus(decision);
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
            result.put("budget", request.getBudget());
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
}
