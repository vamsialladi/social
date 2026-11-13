package com.truehunt.social.service;

import com.truehunt.social.dto.ProfileDto;
import com.truehunt.social.exception.BadRequestException;
import com.truehunt.social.exception.ResourceNotFoundException;
import com.truehunt.social.exception.UnauthorizedException;
import com.truehunt.social.model.ContactAccessRequestEntity;
import com.truehunt.social.repository.ContactAccessRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class ContactAccessService {

    private static final String PENDING = "PENDING";
    private static final String ACCEPTED = "ACCEPTED";
    private static final String REJECTED = "REJECTED";

    private final ContactAccessRequestRepository requestRepository;
    private final ProfileService profileService;

    public ContactAccessService(
            ContactAccessRequestRepository requestRepository,
            ProfileService profileService
    ) {
        this.requestRepository = requestRepository;
        this.profileService = profileService;
    }

    @Transactional
    public Map<String, Object> requestAccess(String targetProfileId, String requesterEmail) {
        if (targetProfileId == null || targetProfileId.isBlank()) {
            throw new BadRequestException("A profile is required for a contact details request.");
        }

        ProfileDto requester = profileService.getMyProfile(requireIdentity(requesterEmail));
        ProfileDto owner = profileService.getProfileByIdForContactAccess(targetProfileId.trim());
        String requesterId = profileId(requester);
        String ownerId = profileId(owner);
        if (sameIdentity(requesterId, ownerId)) {
            throw new BadRequestException("You cannot request access to your own contact details.");
        }

        Optional<ContactAccessRequestEntity> latestRequest = requestRepository
                .findFirstByRequesterProfileIdIgnoreCaseAndOwnerProfileIdIgnoreCaseOrderByCreatedAtDesc(
                        requesterId,
                        ownerId
                );
        if (latestRequest.isPresent()
                && (PENDING.equalsIgnoreCase(latestRequest.get().getStatus())
                    || ACCEPTED.equalsIgnoreCase(latestRequest.get().getStatus()))) {
            return toRequestDto(latestRequest.get(), false);
        }

        ContactAccessRequestEntity request = new ContactAccessRequestEntity();
        request.setRequesterProfileId(requesterId);
        request.setOwnerProfileId(ownerId);
        request.setRequesterName(displayName(requester));
        request.setOwnerName(displayName(owner));
        request.setStatus(PENDING);
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(request.getCreatedAt());
        return toRequestDto(requestRepository.save(request), false);
    }

    public Map<String, Object> getStatus(String targetProfileId, String requesterEmail) {
        ProfileDto requester = profileService.getMyProfile(requireIdentity(requesterEmail));
        ProfileDto owner = profileService.getProfileByIdForContactAccess(targetProfileId);
        String requesterId = profileId(requester);
        String ownerId = profileId(owner);
        Optional<ContactAccessRequestEntity> request = requestRepository
                .findFirstByRequesterProfileIdIgnoreCaseAndOwnerProfileIdIgnoreCaseOrderByCreatedAtDesc(
                        requesterId,
                        ownerId
                );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", request.map(ContactAccessRequestEntity::getStatus).orElse("NONE"));
        response.put("message", requestMessage(request.orElse(null)));
        if (request.isPresent() && ACCEPTED.equalsIgnoreCase(request.get().getStatus())) {
            ProfileDto approvedProfile = profileService.getProfileByIdForContactAccess(ownerId);
            response.put("professionalPhone", approvedProfile.getProfessionalPhone());
            response.put("email", approvedProfile.getEmail());
        }
        return response;
    }

    public List<Map<String, Object>> getIncomingRequests(String ownerEmail) {
        ProfileDto owner = profileService.getMyProfile(requireIdentity(ownerEmail));
        return requestRepository
                .findAllByOwnerProfileIdIgnoreCaseAndStatusOrderByCreatedAtDesc(profileId(owner), PENDING)
                .stream()
                .map(request -> toRequestDto(request, true))
                .toList();
    }

    public List<Map<String, Object>> getOutgoingRequests(String requesterEmail) {
        ProfileDto requester = profileService.getMyProfile(requireIdentity(requesterEmail));
        List<Map<String, Object>> results = new ArrayList<>();
        for (ContactAccessRequestEntity request : requestRepository
                .findAllByRequesterProfileIdIgnoreCaseOrderByCreatedAtDesc(profileId(requester))) {
            results.add(toRequestDto(request, false));
        }
        return results;
    }

    @Transactional
    public Map<String, Object> acceptRequest(Long requestId, String ownerEmail) {
        return decideRequest(requestId, ownerEmail, ACCEPTED);
    }

    @Transactional
    public Map<String, Object> rejectRequest(Long requestId, String ownerEmail) {
        return decideRequest(requestId, ownerEmail, REJECTED);
    }

    private Map<String, Object> decideRequest(Long requestId, String ownerEmail, String decision) {
        ProfileDto owner = profileService.getMyProfile(requireIdentity(ownerEmail));
        ContactAccessRequestEntity request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact access request", "id", requestId));
        if (!sameIdentity(profileId(owner), request.getOwnerProfileId())) {
            throw new UnauthorizedException("You cannot review this contact access request.");
        }
        if (!PENDING.equalsIgnoreCase(request.getStatus())) {
            throw new BadRequestException("This contact access request has already been reviewed.");
        }

        request.setStatus(decision);
        request.setUpdatedAt(LocalDateTime.now());
        return toRequestDto(requestRepository.save(request), true);
    }

    private Map<String, Object> toRequestDto(ContactAccessRequestEntity request, boolean incoming) {
        String status = request.getStatus();
        String message;
        if (incoming) {
            message = request.getRequesterName() + " is asking permission to show your details.";
        } else if (ACCEPTED.equalsIgnoreCase(status)) {
            message = request.getOwnerName() + " accepted your request.";
        } else if (REJECTED.equalsIgnoreCase(status)) {
            message = request.getOwnerName() + " rejected your request.";
        } else {
            message = "Your request is waiting for " + request.getOwnerName() + "'s approval.";
        }

        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", String.valueOf(request.getId()));
        dto.put("profileId", request.getOwnerProfileId());
        dto.put("requesterId", request.getRequesterProfileId());
        dto.put("requesterName", request.getRequesterName());
        dto.put("ownerName", request.getOwnerName());
        dto.put("status", status);
        dto.put("message", message);
        dto.put("createdAt", request.getCreatedAt().toString());
        return dto;
    }

    private String requestMessage(ContactAccessRequestEntity request) {
        if (request == null) {
            return "Contact details are hidden until the profile owner accepts your request.";
        }
        if (ACCEPTED.equalsIgnoreCase(request.getStatus())) {
            return "The profile owner accepted your request. Contact details:";
        }
        if (REJECTED.equalsIgnoreCase(request.getStatus())) {
            return request.getOwnerName() + " rejected your request.";
        }
        return "Your request is waiting for the profile owner's approval.";
    }

    private String profileId(ProfileDto profile) {
        String id = profile.getUserId();
        if (id == null || id.isBlank()) {
            id = profile.getId();
        }
        if (id == null || id.isBlank()) {
            throw new ResourceNotFoundException("Profile", "id", "current");
        }
        return id.trim();
    }

    private String displayName(ProfileDto profile) {
        return profile.getName() == null || profile.getName().isBlank()
                ? "A TrueHunt member"
                : profile.getName().trim();
    }

    private String requireIdentity(String identity) {
        if (identity == null || identity.isBlank()) {
            throw new UnauthorizedException("A valid sign-in is required for contact requests.");
        }
        return identity.trim();
    }

    private boolean sameIdentity(String first, String second) {
        return first != null && second != null
                && first.trim().toLowerCase(Locale.ROOT).equals(second.trim().toLowerCase(Locale.ROOT));
    }
}
