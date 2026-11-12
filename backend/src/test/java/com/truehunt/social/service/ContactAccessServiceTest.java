package com.truehunt.social.service;

import com.truehunt.social.dto.ProfileDto;
import com.truehunt.social.exception.UnauthorizedException;
import com.truehunt.social.model.ContactAccessRequestEntity;
import com.truehunt.social.repository.ContactAccessRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContactAccessServiceTest {

    private static final String REQUESTER_ID = "requester-public-id";
    private static final String OWNER_ID = "owner-public-id";

    private ContactAccessRequestRepository requestRepository;
    private ProfileService profileService;
    private ContactAccessService contactAccessService;

    @BeforeEach
    void setUp() {
        requestRepository = mock(ContactAccessRequestRepository.class);
        profileService = mock(ProfileService.class);
        contactAccessService = new ContactAccessService(requestRepository, profileService);

        ProfileDto requester = profile(REQUESTER_ID, "Casey Requester");
        ProfileDto owner = profile(OWNER_ID, "Riley Owner");
        owner.setPhone("+15551234567");
        owner.setProfessionalPhone("+15559876543");
        owner.setEmail("owner@example.com");
        when(profileService.getMyProfile("requester@example.com")).thenReturn(requester);
        when(profileService.getMyProfile("owner@example.com")).thenReturn(owner);
        when(profileService.getProfileByIdForContactAccess(OWNER_ID)).thenReturn(owner);
    }

    @Test
    void approvedRequestIsTheOnlyStatusThatReturnsContactDetails() {
        ContactAccessRequestEntity request = request("ACCEPTED");
        when(requestRepository
                .findFirstByRequesterProfileIdIgnoreCaseAndOwnerProfileIdIgnoreCaseOrderByCreatedAtDesc(
                        REQUESTER_ID,
                        OWNER_ID
                )).thenReturn(Optional.of(request));

        Map<String, Object> status = contactAccessService.getStatus(OWNER_ID, "requester@example.com");

        assertEquals("ACCEPTED", status.get("status"));
        assertEquals("+15551234567", status.get("phone"));
        assertEquals("+15559876543", status.get("professionalPhone"));
        assertEquals("owner@example.com", status.get("email"));
    }

    @Test
    void rejectedRequestDoesNotReturnContactDetails() {
        ContactAccessRequestEntity request = request("REJECTED");
        when(requestRepository
                .findFirstByRequesterProfileIdIgnoreCaseAndOwnerProfileIdIgnoreCaseOrderByCreatedAtDesc(
                        REQUESTER_ID,
                        OWNER_ID
                )).thenReturn(Optional.of(request));

        Map<String, Object> status = contactAccessService.getStatus(OWNER_ID, "requester@example.com");

        assertEquals("REJECTED", status.get("status"));
        assertEquals("Riley Owner rejected your request.", status.get("message"));
        assertFalse(status.containsKey("phone"));
        assertFalse(status.containsKey("professionalPhone"));
        assertFalse(status.containsKey("email"));
    }

    @Test
    void rejectedRequestCanBeSubmittedAgain() {
        ContactAccessRequestEntity rejectedRequest = request("REJECTED");
        when(requestRepository
                .findFirstByRequesterProfileIdIgnoreCaseAndOwnerProfileIdIgnoreCaseOrderByCreatedAtDesc(
                        REQUESTER_ID,
                        OWNER_ID
                )).thenReturn(Optional.of(rejectedRequest));
        when(requestRepository.save(any(ContactAccessRequestEntity.class)))
                .thenAnswer(invocation -> {
                    ContactAccessRequestEntity saved = invocation.getArgument(0);
                    saved.setId(2L);
                    return saved;
                });

        Map<String, Object> response = contactAccessService.requestAccess(OWNER_ID, "requester@example.com");

        assertEquals("2", response.get("id"));
        assertEquals("PENDING", response.get("status"));
        assertEquals("Your request is waiting for Riley Owner's approval.", response.get("message"));
        verify(requestRepository).save(any(ContactAccessRequestEntity.class));
    }

    @Test
    void onlyTheProfileOwnerCanAcceptARequest() {
        ContactAccessRequestEntity request = request("PENDING");
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        assertThrows(UnauthorizedException.class, () -> contactAccessService.acceptRequest(
                1L,
                "requester@example.com"
        ));
    }

    @Test
    void ownerDecisionUpdatesRequestStatus() {
        ContactAccessRequestEntity request = request("PENDING");
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(ContactAccessRequestEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        contactAccessService.acceptRequest(1L, "owner@example.com");

        assertEquals("ACCEPTED", request.getStatus());
        verify(requestRepository).save(request);
    }

    private static ProfileDto profile(String id, String name) {
        ProfileDto profile = new ProfileDto();
        profile.setUserId(id);
        profile.setName(name);
        return profile;
    }

    private static ContactAccessRequestEntity request(String status) {
        ContactAccessRequestEntity request = new ContactAccessRequestEntity();
        request.setId(1L);
        request.setRequesterProfileId(REQUESTER_ID);
        request.setOwnerProfileId(OWNER_ID);
        request.setRequesterName("Casey Requester");
        request.setOwnerName("Riley Owner");
        request.setStatus(status);
        request.setCreatedAt(LocalDateTime.now());
        return request;
    }
}
