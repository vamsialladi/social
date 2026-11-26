package com.truehunt.social.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.truehunt.social.exception.BadRequestException;
import com.truehunt.social.exception.ConflictException;
import com.truehunt.social.exception.ResourceNotFoundException;
import com.truehunt.social.exception.UnauthorizedException;
import com.truehunt.social.model.EndorsementEntity;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.repository.EndorsementRepository;
import com.truehunt.social.repository.ProfileRepository;
import com.truehunt.social.repository.UserAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class EndorsementService {

    private static final int MAX_PAYLOAD_LENGTH = 32_768;
    private static final int MAX_MESSAGE_LENGTH = 5_000;
    private static final Set<String> SERVER_MANAGED_FIELDS = Set.of(
            "id",
            "endorsementId",
            "endorserProfileId",
            "endorserId",
            "authorId",
            "endorserName",
            "endorserProfileImage",
            "endorserAvatar",
            "endorserProfession",
            "receiverProfileId",
            "receiverName",
            "receiverProfileImage",
            "createdAt",
            "updatedAt"
    );

    private static final TypeReference<LinkedHashMap<String, Object>> PAYLOAD_TYPE = new TypeReference<>() {};

    private final EndorsementRepository endorsementRepository;
    private final ProfileRepository profileRepository;
    private final UserAccountRepository userAccountRepository;
    private final ObjectMapper objectMapper;

    public EndorsementService(
            EndorsementRepository endorsementRepository,
            ProfileRepository profileRepository,
            UserAccountRepository userAccountRepository,
            ObjectMapper objectMapper
    ) {
        this.endorsementRepository = endorsementRepository;
        this.profileRepository = profileRepository;
        this.userAccountRepository = userAccountRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getProfileEndorsements(String receiverIdentifier, String viewerIdentifier) {
        ProfileIdentity receiver;
        try {
            receiver = resolveProfile(receiverIdentifier);
        } catch (ResourceNotFoundException e) {
            return List.of();
        }
        String viewerProfileId = resolveViewerProfileId(viewerIdentifier);

        return endorsementRepository
                .findAllByReceiverProfileIdIgnoreCaseOrderByCreatedAtDesc(receiver.userId())
                .stream()
                .filter(endorsement -> isPublic(endorsement) || canViewPrivate(endorsement, receiver, viewerProfileId))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public Map<String, Object> createEndorsement(
            String receiverIdentifier,
            Map<String, Object> request,
            String endorserIdentifier
    ) {
        ProfileIdentity receiver = resolveProfile(receiverIdentifier);
        ProfileIdentity endorser = resolveEndorser(endorserIdentifier);
        if (sameIdentity(receiver.userId(), endorser.userId())) {
            throw new BadRequestException("You cannot endorse your own profile.");
        }
        if (endorsementRepository.existsByReceiverProfileIdIgnoreCaseAndEndorserProfileIdIgnoreCase(
                receiver.userId(),
                endorser.userId()
        )) {
            throw new ConflictException("You have already endorsed this profile.");
        }

        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> payload = preparePayload(request, receiver, endorser, now.toString(), now);
        EndorsementEntity endorsement = new EndorsementEntity();
        endorsement.setReceiverProfileId(receiver.userId());
        endorsement.setEndorserProfileId(endorser.userId());
        endorsement.setPayloadJson(serializePayload(payload));
        endorsement.setCreatedAt(now);
        endorsement.setUpdatedAt(now);

        try {
            return toResponse(endorsementRepository.saveAndFlush(endorsement));
        } catch (DataIntegrityViolationException error) {
            throw new ConflictException("You have already endorsed this profile.");
        }
    }

    @Transactional
    public Map<String, Object> updateEndorsement(
            String receiverIdentifier,
            Long endorsementId,
            Map<String, Object> request,
            String endorserIdentifier
    ) {
        ProfileIdentity receiver = resolveProfile(receiverIdentifier);
        ProfileIdentity endorser = resolveEndorser(endorserIdentifier);
        EndorsementEntity endorsement = findEndorsement(receiver, endorsementId);
        if (!sameIdentity(endorsement.getEndorserProfileId(), endorser.userId())) {
            throw new UnauthorizedException("Only the person who submitted this endorsement can update it.");
        }

        Map<String, Object> oldPayload = readPayload(endorsement);
        Object createdDate = oldPayload.getOrDefault("createdDate", endorsement.getCreatedAt().toString());
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> payload = preparePayload(request, receiver, endorser, String.valueOf(createdDate), now);
        endorsement.setPayloadJson(serializePayload(payload));
        endorsement.setUpdatedAt(now);
        return toResponse(endorsementRepository.save(endorsement));
    }

    @Transactional
    public void deleteEndorsement(String receiverIdentifier, Long endorsementId, String endorserIdentifier) {
        ProfileIdentity receiver = resolveProfile(receiverIdentifier);
        ProfileIdentity endorser = resolveEndorser(endorserIdentifier);
        EndorsementEntity endorsement = findEndorsement(receiver, endorsementId);
        if (!sameIdentity(endorsement.getEndorserProfileId(), endorser.userId())
                && !sameIdentity(endorsement.getReceiverProfileId(), endorser.userId())) {
            throw new UnauthorizedException("Only the person who submitted this endorsement or the profile owner can delete it.");
        }
        endorsementRepository.delete(endorsement);
    }

    private EndorsementEntity findEndorsement(ProfileIdentity receiver, Long endorsementId) {
        if (endorsementId == null) {
            throw new BadRequestException("A valid endorsement ID is required.");
        }
        return endorsementRepository
                .findByIdAndReceiverProfileIdIgnoreCase(endorsementId, receiver.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Endorsement", "id", endorsementId));
    }

    private Map<String, Object> preparePayload(
            Map<String, Object> request,
            ProfileIdentity receiver,
            ProfileIdentity endorser,
            String createdDate,
            LocalDateTime updatedAt
    ) {
        if (request == null) {
            throw new BadRequestException("Endorsement details are required.");
        }
        Object rawMessage = request.get("endorsementMessage");
        String message = rawMessage == null ? "" : rawMessage.toString().trim();
        if (message.isBlank()) {
            throw new BadRequestException("An endorsement message is required.");
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new BadRequestException("The endorsement message is too long.");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        request.forEach((key, value) -> {
            if (key != null && !SERVER_MANAGED_FIELDS.contains(key)) {
                payload.put(key, value);
            }
        });
        payload.put("endorsementMessage", message);
        payload.put("endorserName", endorser.name());
        payload.put("endorserProfileId", endorser.userId());
        payload.put("endorserProfileImage", endorser.profileImage());
        payload.put("endorserProfession", endorser.profession());
        payload.put("receiverName", receiver.name());
        payload.put("receiverProfileId", receiver.userId());
        payload.put("receiverProfileImage", receiver.profileImage());
        payload.put("createdDate", createdDate);
        payload.put("updatedDate", updatedAt.toString());
        return payload;
    }

    private String serializePayload(Map<String, Object> payload) {
        try {
            String serialized = objectMapper.writeValueAsString(payload);
            if (serialized.length() > MAX_PAYLOAD_LENGTH) {
                throw new BadRequestException("Endorsement details are too large.");
            }
            return serialized;
        } catch (BadRequestException error) {
            throw error;
        } catch (Exception error) {
            throw new BadRequestException("Endorsement details could not be saved.");
        }
    }

    private Map<String, Object> toResponse(EndorsementEntity endorsement) {
        Map<String, Object> response = readPayload(endorsement);
        String id = String.valueOf(endorsement.getId());
        response.put("id", id);
        response.put("endorsementId", id);
        response.put("receiverProfileId", endorsement.getReceiverProfileId());
        response.put("endorserProfileId", endorsement.getEndorserProfileId());
        response.put("createdAt", endorsement.getCreatedAt().toString());
        response.put("updatedAt", endorsement.getUpdatedAt().toString());
        return response;
    }

    private Map<String, Object> readPayload(EndorsementEntity endorsement) {
        try {
            return objectMapper.readValue(endorsement.getPayloadJson(), PAYLOAD_TYPE);
        } catch (Exception error) {
            throw new IllegalStateException("Stored endorsement data could not be read.", error);
        }
    }

    private boolean isPublic(EndorsementEntity endorsement) {
        return "public".equalsIgnoreCase(String.valueOf(readPayload(endorsement).get("visibility")));
    }

    private boolean canViewPrivate(
            EndorsementEntity endorsement,
            ProfileIdentity receiver,
            String viewerProfileId
    ) {
        return viewerProfileId != null
                && (sameIdentity(receiver.userId(), viewerProfileId)
                || sameIdentity(endorsement.getEndorserProfileId(), viewerProfileId));
    }

    private ProfileIdentity resolveProfile(String identifier) {
        String clean = cleanIdentifier(identifier);
        ProfileEntity profile = profileRepository.findByUserId(clean)
                .or(() -> profileRepository.findByEmail(clean))
                .orElse(null);
        UserAccountEntity account = userAccountRepository.findByPublicUserId(clean)
                .or(() -> userAccountRepository.findByEmail(clean))
                .orElse(null);
        if (profile == null && account == null) {
            throw new ResourceNotFoundException("Profile", "id", clean);
        }
        return profileIdentity(profile, account);
    }

    private ProfileIdentity resolveEndorser(String identifier) {
        String clean = cleanIdentifier(identifier);
        UserAccountEntity account = userAccountRepository.findByEmail(clean)
                .or(() -> userAccountRepository.findByPublicUserId(clean))
                .orElseThrow(() -> new UnauthorizedException("A valid sign-in is required to endorse a profile."));
        ProfileEntity profile = profileRepository.findByEmail(account.getEmail())
                .or(() -> profileRepository.findByUserId(account.getPublicUserId()))
                .orElse(null);
        return profileIdentity(profile, account);
    }

    private String resolveViewerProfileId(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return null;
        }
        UserAccountEntity account = userAccountRepository.findByEmail(identifier.trim())
                .or(() -> userAccountRepository.findByPublicUserId(identifier.trim()))
                .orElse(null);
        if (account == null) {
            return null;
        }
        ProfileEntity profile = profileRepository.findByEmail(account.getEmail())
                .or(() -> profileRepository.findByUserId(account.getPublicUserId()))
                .orElse(null);
        return profileIdentity(profile, account).userId();
    }

    private ProfileIdentity profileIdentity(ProfileEntity profile, UserAccountEntity account) {
        String userId = firstNonblank(
                account == null ? null : account.getPublicUserId(),
                profile == null ? null : profile.getUserId(),
                account == null ? null : account.getEmail(),
                profile == null ? null : profile.getEmail()
        );
        if (userId == null) {
            throw new ResourceNotFoundException("Profile", "id", "unknown");
        }
        String name = firstNonblank(
                profile == null ? null : profile.getName(),
                account == null ? null : account.getName()
        );
        String profession = firstNonblank(
                profile == null ? null : profile.getPrimaryCraft(),
                profile == null ? null : profile.getSubCraft(),
                account == null ? null : account.getPrimaryCraft(),
                account == null ? null : account.getSubCraft()
        );
        String profileImage = profile == null ? null : profile.getProfileImageUrl();
        return new ProfileIdentity(userId, name == null ? "" : name, profession == null ? "" : profession,
                profileImage == null ? "" : profileImage);
    }

    private String cleanIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new BadRequestException("A profile ID is required.");
        }
        return identifier.trim();
    }

    private String firstNonblank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private boolean sameIdentity(String first, String second) {
        return first != null && second != null
                && first.trim().toLowerCase(Locale.ROOT).equals(second.trim().toLowerCase(Locale.ROOT));
    }

    private record ProfileIdentity(String userId, String name, String profession, String profileImage) {}
}