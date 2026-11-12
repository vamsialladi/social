package com.truehunt.social.service;

import com.truehunt.social.dto.CreatePostRequest;
import com.truehunt.social.dto.InvestorAccessRequestDto;
import com.truehunt.social.exception.UnauthorizedException;
import com.truehunt.social.model.InvestorAccessRequestEntity;
import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.repository.InvestorAccessRequestRepository;
import com.truehunt.social.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InvestorAccessServiceTest {

    @Test
    void publishesInvestorProfileAndCreatesFeedPost() {
        InvestorAccessRequestRepository requestRepository = mock(InvestorAccessRequestRepository.class);
        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        PostService postService = mock(PostService.class);
        String email = "investor@example.com";
        UserAccountEntity account = new UserAccountEntity();
        account.setId(7L);
        account.setEmail(email);
        account.setPublicUserId("THSC12345");

        when(userAccountRepository.findByEmail(email)).thenReturn(Optional.of(account));
        when(requestRepository.findFirstByApplicantEmailIgnoreCaseOrderByCreatedAtDesc(email))
                .thenReturn(Optional.empty());
        when(requestRepository.save(any(InvestorAccessRequestEntity.class))).thenAnswer(invocation -> {
            InvestorAccessRequestEntity entity = invocation.getArgument(0);
            entity.setId(12L);
            return entity;
        });
        when(postService.createPost(any(CreatePostRequest.class), eq(email)))
                .thenReturn(Map.of("id", 21L));

        InvestorAccessRequestDto request = new InvestorAccessRequestDto();
        request.setName("Ava Investor");
        request.setBudget("$100,000");
        request.setRole("Investor");
        request.setDescription("Backing independent film projects.");
        request.setConsentAccepted(true);

        Map<String, Object> result = new InvestorAccessService(
                requestRepository,
                userAccountRepository,
                postService
        ).submit(email, request);

        assertEquals("PUBLISHED", result.get("status"));
        assertEquals("", result.get("mobileNumber"));
        assertEquals("Backing independent film projects.", result.get("description"));
        assertEquals(Map.of("id", 21L), result.get("feedPost"));

        ArgumentCaptor<CreatePostRequest> feedPostCaptor = ArgumentCaptor.forClass(CreatePostRequest.class);
        verify(postService).createPost(feedPostCaptor.capture(), eq(email));
        assertEquals("post", feedPostCaptor.getValue().getType());
        assertEquals(12L, feedPostCaptor.getValue().getInvestorProfileId());
        assertEquals("Role: Investor\nInvestment budget: $100,000\n\nBacking independent film projects.",
                feedPostCaptor.getValue().getContent());
    }

    @Test
    void publicListingsExcludeContactAndAccountIdentifiers() {
        InvestorAccessRequestRepository requestRepository = mock(InvestorAccessRequestRepository.class);
        InvestorAccessRequestEntity entity = new InvestorAccessRequestEntity();
        entity.setId(12L);
        entity.setName("Ava Investor");
        entity.setApplicantEmail("investor@example.com");
        entity.setUserId("THSC12345");
        entity.setMobileNumber("5551234567");
        entity.setRole("Investor");
        entity.setDescription("Backing independent film projects.");
        entity.setBudget("$100,000");
        entity.setCreatedAt(LocalDateTime.now());
        when(requestRepository.findAllByStatusOrderByCreatedAtDesc("PUBLISHED")).thenReturn(List.of(entity));

        List<Map<String, Object>> listings = new InvestorAccessService(
                requestRepository,
                mock(UserAccountRepository.class),
                mock(PostService.class)
        ).getPublishedListings("viewer@example.com");

        Map<String, Object> listing = listings.get(0);
        assertEquals("Ava Investor", listing.get("name"));
        assertEquals("Investor", listing.get("role"));
        assertEquals("Backing independent film projects.", listing.get("description"));
        assertEquals("$100,000", listing.get("budget"));
        assertFalse(listing.containsKey("mobileNumber"));
        assertFalse(listing.containsKey("email"));
        assertFalse(listing.containsKey("userId"));
        assertFalse(listing.containsKey("feedPostId"));
        assertFalse((Boolean) listing.get("isMine"));
    }

    @Test
    void publicListingsDoNotExposeBudgetWhenItDuplicatesTheMobileNumber() {
        InvestorAccessRequestRepository requestRepository = mock(InvestorAccessRequestRepository.class);
        InvestorAccessRequestEntity entity = new InvestorAccessRequestEntity();
        entity.setId(12L);
        entity.setApplicantEmail("investor@example.com");
        entity.setName("Ava Investor");
        entity.setMobileNumber("+1 (555) 123-4567");
        entity.setBudget("5551234567");
        entity.setRole("Investor");
        entity.setDescription("Backing independent film projects.");
        when(requestRepository.findAllByStatusOrderByCreatedAtDesc("PUBLISHED")).thenReturn(List.of(entity));

        Map<String, Object> listing = new InvestorAccessService(
                requestRepository,
                mock(UserAccountRepository.class),
                mock(PostService.class)
        ).getPublishedListings("viewer@example.com").get(0);

        assertFalse(listing.containsKey("budget"));
        assertFalse(listing.containsKey("mobileNumber"));
    }

    @Test
    void deletingPublishedPostRemovesItsListing() {
        InvestorAccessRequestRepository requestRepository = mock(InvestorAccessRequestRepository.class);
        InvestorAccessRequestEntity entity = new InvestorAccessRequestEntity();
        entity.setId(12L);
        entity.setApplicantEmail("investor@example.com");
        entity.setStatus("PUBLISHED");
        entity.setFeedPostId(21L);
        when(requestRepository.findFirstByFeedPostIdAndApplicantEmailIgnoreCase(21L, "investor@example.com"))
                .thenReturn(Optional.of(entity));

        boolean withdrawn = new InvestorAccessService(
                requestRepository,
                mock(UserAccountRepository.class),
                mock(PostService.class)
        ).deletePublishedListingForDeletedPost("21", "investor@example.com");

        assertTrue(withdrawn);
        verify(requestRepository).delete(entity);
        verify(requestRepository, never()).save(entity);
    }

    @Test
    void ownerCanDeleteListingAndLinkedFeedPost() {
        InvestorAccessRequestRepository requestRepository = mock(InvestorAccessRequestRepository.class);
        PostService postService = mock(PostService.class);
        InvestorAccessRequestEntity entity = new InvestorAccessRequestEntity();
        entity.setId(12L);
        entity.setApplicantEmail("investor@example.com");
        entity.setStatus("PUBLISHED");
        entity.setFeedPostId(21L);
        when(requestRepository.findById(12L)).thenReturn(Optional.of(entity));

        Map<String, Object> result = new InvestorAccessService(
                requestRepository,
                mock(UserAccountRepository.class),
                postService
        ).deletePublishedListing(12L, "investor@example.com");

        assertEquals(12L, result.get("id"));
        assertEquals(21L, result.get("feedPostId"));
        verify(postService).deletePostIfPresent("21", "investor@example.com");
        verify(requestRepository).delete(entity);
    }

    @Test
    void otherUsersCannotDeleteInvestorListing() {
        InvestorAccessRequestRepository requestRepository = mock(InvestorAccessRequestRepository.class);
        InvestorAccessRequestEntity entity = new InvestorAccessRequestEntity();
        entity.setId(12L);
        entity.setApplicantEmail("investor@example.com");
        when(requestRepository.findById(12L)).thenReturn(Optional.of(entity));
        PostService postService = mock(PostService.class);

        InvestorAccessService service = new InvestorAccessService(
                requestRepository,
                mock(UserAccountRepository.class),
                postService
        );

        assertThrows(UnauthorizedException.class, () -> service.deletePublishedListing(12L, "other@example.com"));
        verify(postService, never()).deletePostIfPresent(any(), any());
        verify(requestRepository, never()).delete(entity);
    }
}
