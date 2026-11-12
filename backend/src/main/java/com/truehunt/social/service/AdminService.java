package com.truehunt.social.service;

import com.truehunt.social.exception.BadRequestException;
import com.truehunt.social.exception.ResourceNotFoundException;
import com.truehunt.social.model.PostEntity;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.repository.NetworkProfileRepository;
import com.truehunt.social.repository.OpportunityApplicationRepository;
import com.truehunt.social.repository.PostRepository;
import com.truehunt.social.repository.ProfileRepository;
import com.truehunt.social.repository.ProfileViewRepository;
import com.truehunt.social.repository.UserAccountRepository;
import com.truehunt.social.repository.UserProfileRelationshipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class AdminService {

    private final UserAccountRepository userAccountRepository;
    private final ProfileRepository profileRepository;
    private final PostRepository postRepository;
    private final OpportunityApplicationRepository applicationRepository;
    private final NetworkProfileRepository networkProfileRepository;
    private final UserProfileRelationshipRepository relationshipRepository;
    private final ProfileViewRepository profileViewRepository;

    public AdminService(UserAccountRepository userAccountRepository,
                        ProfileRepository profileRepository,
                        PostRepository postRepository,
                        OpportunityApplicationRepository applicationRepository,
                        NetworkProfileRepository networkProfileRepository,
                        UserProfileRelationshipRepository relationshipRepository,
                        ProfileViewRepository profileViewRepository) {
        this.userAccountRepository = userAccountRepository;
        this.profileRepository = profileRepository;
        this.postRepository = postRepository;
        this.applicationRepository = applicationRepository;
        this.networkProfileRepository = networkProfileRepository;
        this.relationshipRepository = relationshipRepository;
        this.profileViewRepository = profileViewRepository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAccounts() {
        return userAccountRepository.findAll().stream()
                .filter(account -> !"ADMIN".equalsIgnoreCase(account.getRole()))
                .map(account -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("id", account.getId());
                    result.put("userId", account.getPublicUserId());
                    result.put("name", account.getName());
                    result.put("email", account.getEmail());
                    result.put("role", account.getRole());
                    result.put("primaryCraft", account.getCraft());
                    result.put("createdAt", account.getCreatedAt());
                    return result;
                })
                .toList();
    }

    @Transactional
    public void deletePost(Long postId) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
        applicationRepository.deleteAllByPostIdIn(List.of(postId));
        postRepository.delete(post);
    }

    @Transactional
    public void deleteAccount(Long accountId, String currentAdminEmail) {
        UserAccountEntity account = userAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", accountId));

        if (account.getEmail() != null && account.getEmail().equalsIgnoreCase(currentAdminEmail)) {
            throw new BadRequestException("You cannot delete the administrator account you are currently using.");
        }

        Set<String> identities = new LinkedHashSet<>();
        addIdentity(identities, account.getEmail());
        addIdentity(identities, account.getPublicUserId());

        Map<Long, ProfileEntity> profilesById = new LinkedHashMap<>();
        if (account.getEmail() != null) {
            profileRepository.findAllByEmail(account.getEmail())
                    .forEach(profile -> profilesById.put(profile.getId(), profile));
        }
        if (account.getPublicUserId() != null && !account.getPublicUserId().isBlank()) {
            profileRepository.findAllByUserId(account.getPublicUserId())
                    .forEach(profile -> profilesById.put(profile.getId(), profile));
        }
        profilesById.values().forEach(profile -> addIdentity(identities, profile.getUserId()));

        List<PostEntity> authoredPosts = postRepository.findAllByAuthorIdIn(identities);
        List<Long> authoredPostIds = authoredPosts.stream().map(PostEntity::getId).toList();
        if (!authoredPostIds.isEmpty()) {
            applicationRepository.deleteAllByPostIdIn(authoredPostIds);
            postRepository.deleteAll(authoredPosts);
        }
        applicationRepository.deleteAllByApplicantIdentityIn(identities);

        relationshipRepository.deleteAllByOwnerUserIdInOrTargetProfileIdIn(identities, identities);
        profileViewRepository.deleteAllByOwnerUserIdInOrViewerUserIdIn(identities, identities);
        networkProfileRepository.deleteAllById(identities);
        profileRepository.deleteAll(new ArrayList<>(profilesById.values()));
        userAccountRepository.delete(account);
    }

    private void addIdentity(Collection<String> identities, String value) {
        if (value == null || value.isBlank()) return;
        identities.add(value.trim());
        identities.add(value.trim().toLowerCase(Locale.ROOT));
    }
}
