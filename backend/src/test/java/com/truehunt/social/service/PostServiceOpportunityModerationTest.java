package com.truehunt.social.service;

import com.truehunt.social.dto.CreatePostRequest;
import com.truehunt.social.exception.BadRequestException;
import com.truehunt.social.exception.UnauthorizedException;
import com.truehunt.social.model.OpportunityApplicationEntity;
import com.truehunt.social.model.NotificationEntity;
import com.truehunt.social.model.PostEntity;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.repository.NotificationRepository;
import com.truehunt.social.repository.OpportunityApplicationRepository;
import com.truehunt.social.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

class PostServiceOpportunityModerationTest {

    private PostRepository postRepository;
    private OpportunityApplicationRepository opportunityApplicationRepository;
    private NotificationRepository notificationRepository;
    private ProfileService profileService;
    private PostService postService;
    private ProfileEntity ownerProfile;

    @BeforeEach
    void setUp() {
        postRepository = mock(PostRepository.class);
        opportunityApplicationRepository = mock(OpportunityApplicationRepository.class);
        notificationRepository = mock(NotificationRepository.class);
        profileService = mock(ProfileService.class);
        postService = new PostService(
                postRepository,
                opportunityApplicationRepository,
                notificationRepository,
                profileService
        );

        ownerProfile = new ProfileEntity();
        ownerProfile.setUserId("owner-1");
        ownerProfile.setEmail("owner@example.com");
        when(profileService.findCurrentProfile("owner@example.com")).thenReturn(ownerProfile);
        when(postRepository.save(any(PostEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void savesTextOnlyOpportunity() {
        CreatePostRequest request = new CreatePostRequest();
        request.setType("opportunity");
        request.setTitle("Upcoming Film");
        request.setContent("Hero / Lead · Feature Film");
        request.setOpportunityDetails(Map.of("actingRole", "Hero / Lead"));

        Map<String, Object> created = postService.createPost(request, "owner@example.com");

        assertEquals("opportunity", created.get("type"));
        assertEquals("Upcoming Film", created.get("title"));
        assertEquals(Map.of("actingRole", "Hero / Lead"), created.get("opportunityDetails"));
        assertNull(created.get("media"));
        verify(postRepository).save(any(PostEntity.class));
    }

    @Test
    void preservesOpportunityImageSeparatelyFromDetails() {
        CreatePostRequest request = new CreatePostRequest();
        request.setType("opportunity");
        request.setContent("Casting call");
        request.setMedia(Map.of("url", "/uploads/audition.jpg", "kind", "image"));
        Map<String, Object> details = Map.of(
                "actingRole", "Hero / Lead",
                "languagesKnown", List.of("Telugu", "English"),
                "guardianPermission", false
        );
        request.setOpportunityDetails(details);

        Map<String, Object> created = postService.createPost(request, "owner@example.com");

        assertEquals("/uploads/audition.jpg", ((Map<?, ?>) created.get("media")).get("url"));
        assertEquals(details, created.get("opportunityDetails"));
    }

    @Test
    void updatesOpportunityWithoutAProjectName() {
        PostEntity existing = new PostEntity();
        existing.setId(15L);
        existing.setAuthorId(ownerProfile.getUserId());
        existing.setType("opportunity");
        when(postRepository.findById(15L)).thenReturn(Optional.of(existing));

        CreatePostRequest request = new CreatePostRequest();
        request.setTitle("");
        request.setContent("Feature Film");
        request.setOpportunityDetails(Map.of("projectName", ""));

        Map<String, Object> updated = postService.updateOpportunity("15", request, "owner@example.com");

        assertEquals("", updated.get("title"));
        verify(postRepository).save(existing);
    }

    @Test
    void acceptsImageOnlyPost() {
        CreatePostRequest request = new CreatePostRequest();
        request.setType("post");
        request.setMedia(Map.of("url", "/uploads/photo.jpg", "kind", "image"));

        Map<String, Object> created = postService.createPost(request, "owner@example.com");

        assertEquals("", created.get("content"));
        assertEquals("/uploads/photo.jpg", ((Map<?, ?>) created.get("media")).get("url"));
        verify(postRepository).save(any(PostEntity.class));
    }

    @Test
    void rejectsPostWithoutTextOrMediaUrl() {
        CreatePostRequest request = new CreatePostRequest();
        request.setType("post");
        request.setMedia(Map.of("kind", "image"));

        assertThrows(
                BadRequestException.class,
                () -> postService.createPost(request, "owner@example.com")
        );
    }

    @Test
    void appliesOncePerApplicantAndReturnsPersonalAppliedState() {
        PostEntity opportunity = new PostEntity();
        opportunity.setId(42L);
        opportunity.setType("opportunity");
        opportunity.setApplicationsCount(3);
        when(postRepository.findById(42L)).thenReturn(Optional.of(opportunity));
        when(opportunityApplicationRepository.existsByPostIdAndApplicantIdentity(42L, "applicant@example.com"))
                .thenReturn(false);
        when(opportunityApplicationRepository.save(any(OpportunityApplicationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> response = postService.applyToOpportunity("42", " APPLICANT@example.com ");

        assertEquals(true, response.get("applied"));
        assertEquals(4, response.get("applicationsCount"));
        verify(opportunityApplicationRepository).save(any(OpportunityApplicationEntity.class));
    }

    @Test
    void notifiesApplicantAndCastingPostOwnerWhenApplicationIsSubmitted() {
        PostEntity opportunity = new PostEntity();
        opportunity.setId(42L);
        opportunity.setType("opportunity");
        opportunity.setTitle("Fashion Model Required");
        opportunity.setAuthorId("owner-profile-1");
        opportunity.setAuthorName("Studio Producer");
        opportunity.setOpportunityDetails(Map.of("projectCampaignName", "Summer Campaign"));
        when(profileService.findCurrentProfile("owner-profile-1")).thenReturn(ownerProfile);
        when(postRepository.findById(42L)).thenReturn(Optional.of(opportunity));
        when(opportunityApplicationRepository.existsByPostIdAndApplicantIdentity(42L, "applicant@example.com"))
                .thenReturn(false);
        when(opportunityApplicationRepository.save(any(OpportunityApplicationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(postRepository.save(any(PostEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        postService.applyToOpportunity(
                "42",
                "applicant@example.com",
                Map.of("applicantName", "Nia Rao")
        );

        var notificationCaptor = forClass(NotificationEntity.class);
        verify(notificationRepository, times(2)).save(notificationCaptor.capture());
        List<NotificationEntity> notifications = notificationCaptor.getAllValues();
        NotificationEntity applicantNotification = notifications.stream()
                .filter(notification -> "applicant@example.com".equals(notification.getRecipientId()))
                .findFirst()
                .orElseThrow();
        NotificationEntity ownerNotification = notifications.stream()
                .filter(notification -> "owner@example.com".equals(notification.getRecipientId()))
                .findFirst()
                .orElseThrow();
        assertEquals("Application Sent", applicantNotification.getTitle());
        assertEquals(true, applicantNotification.getMessage().contains("Fashion Model Required"));
        assertEquals("New Application Received", ownerNotification.getTitle());
        assertEquals(true, ownerNotification.getMessage().contains("Nia Rao"));
    }

    @Test
    void cancelsOnlyTheMatchingApplicantsApplication() {
        PostEntity opportunity = new PostEntity();
        opportunity.setId(42L);
        opportunity.setType("opportunity");
        opportunity.setApplicationsCount(3);
        OpportunityApplicationEntity application =
                new OpportunityApplicationEntity(42L, "applicant@example.com");
        when(postRepository.findById(42L)).thenReturn(Optional.of(opportunity));
        when(opportunityApplicationRepository.findByPostIdAndApplicantIdentity(42L, "applicant@example.com"))
                .thenReturn(Optional.of(application));

        Map<String, Object> response = postService.cancelOpportunityApplication("42", "APPLICANT@example.com");

        assertEquals(false, response.get("applied"));
        assertEquals(2, response.get("applicationsCount"));
        verify(opportunityApplicationRepository).delete(application);
    }

    @Test
    void ownerCanViewApplicantNameAndApplicationDetails() {
        PostEntity opportunity = new PostEntity();
        opportunity.setId(42L);
        opportunity.setAuthorId("owner@example.com");
        opportunity.setType("opportunity");
        OpportunityApplicationEntity application =
                new OpportunityApplicationEntity(42L, "applicant@example.com");
        application.setId(9L);
        application.setApplicantName("Applicant Name");
        application.setNote("{\"currentLocation\":\"Hyderabad\"}");
        when(postRepository.findById(42L)).thenReturn(Optional.of(opportunity));
        when(opportunityApplicationRepository.findAllByPostIdOrderByAppliedAtDesc(42L))
                .thenReturn(List.of(application));

        List<Map<String, Object>> applications =
                postService.getOpportunityApplications("42", "owner@example.com");

        assertEquals(1, applications.size());
        assertEquals("Applicant Name", applications.get(0).get("applicantName"));
        assertEquals("applicant@example.com", applications.get(0).get("applicantId"));
        assertEquals(
                "Hyderabad",
                ((Map<?, ?>) applications.get(0).get("applicationDetails")).get("currentLocation")
        );
        assertEquals(false, applications.get(0).get("shortlisted"));
    }

    @Test
    void nonOwnerCannotViewCastingApplications() {
        PostEntity opportunity = new PostEntity();
        opportunity.setId(42L);
        opportunity.setAuthorId("owner@example.com");
        opportunity.setType("opportunity");
        when(postRepository.findById(42L)).thenReturn(Optional.of(opportunity));

        assertThrows(
                UnauthorizedException.class,
                () -> postService.getOpportunityApplications("42", "other@example.com")
        );
    }

    @Test
    void ownerCanChangeShortlistStatusForApplicationOnTheirPost() {
        PostEntity opportunity = new PostEntity();
        opportunity.setId(42L);
        opportunity.setAuthorId("owner@example.com");
        opportunity.setType("opportunity");
        OpportunityApplicationEntity application =
                new OpportunityApplicationEntity(42L, "applicant@example.com");
        application.setId(9L);
        when(postRepository.findById(42L)).thenReturn(Optional.of(opportunity));
        when(opportunityApplicationRepository.findByIdAndPostId(9L, 42L))
                .thenReturn(Optional.of(application));
        when(opportunityApplicationRepository.save(any(OpportunityApplicationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> response = postService.updateOpportunityApplicationShortlist(
                "42",
                "9",
                true,
                "owner@example.com"
        );

        assertEquals(true, response.get("shortlisted"));
        verify(opportunityApplicationRepository).save(application);
    }

    @Test
    void opportunityDtoUsesTheCurrentApplicantsApplicationRecord() {
        PostEntity opportunity = new PostEntity();
        opportunity.setId(42L);
        opportunity.setType("opportunity");
        opportunity.setApplied(true);
        when(postRepository.findAllByTypeIgnoreCaseOrderByCreatedAtDesc("opportunity"))
                .thenReturn(List.of(opportunity));
        when(opportunityApplicationRepository.existsByPostIdAndApplicantIdentity(42L, "applicant@example.com"))
                .thenReturn(false);

        List<Map<String, Object>> opportunities = postService.getOpportunities("applicant@example.com");

        assertEquals(false, opportunities.get(0).get("applied"));
    }

    @Test
    void updatesOwnerPostTextAndMedia() {
        PostEntity existing = new PostEntity();
        existing.setId(12L);
        existing.setAuthorId(ownerProfile.getUserId());
        existing.setType("post");
        existing.setTitle("Old title");
        existing.setContent("Old text");
        existing.setMedia(Map.of("url", "/uploads/old.jpg", "kind", "image"));
        when(postRepository.findById(12L)).thenReturn(Optional.of(existing));

        CreatePostRequest request = new CreatePostRequest();
        request.setTitle("New title");
        request.setContent("New text");
        request.setOpportunityDetails(Map.of("fullName", "Updated casting name", "age", "31"));
        request.setMedia(Map.of("url", "/uploads/new.mp4", "kind", "video"));

        Map<String, Object> updated = postService.updatePost("12", request, "owner@example.com");

        assertEquals("New title", updated.get("title"));
        assertEquals("New text", updated.get("content"));
        assertEquals(
                "Updated casting name",
                ((Map<?, ?>) updated.get("opportunityDetails")).get("fullName")
        );
        assertEquals("/uploads/new.mp4", ((Map<?, ?>) updated.get("media")).get("url"));
        assertEquals("video", ((Map<?, ?>) updated.get("media")).get("kind"));
        verify(postRepository).save(existing);
    }

    @Test
    void deletesOpportunityForItsOwner() {
        PostEntity existing = new PostEntity();
        existing.setId(13L);
        existing.setAuthorId(ownerProfile.getUserId());
        existing.setType("opportunity");
        when(postRepository.findById(13L)).thenReturn(Optional.of(existing));

        postService.deletePost("13", "owner@example.com");

        verify(postRepository).delete(existing);
    }

    @Test
    void rejectsDeletingAnotherUsersPost() {
        PostEntity existing = new PostEntity();
        existing.setId(14L);
        existing.setAuthorId("different-owner");
        when(postRepository.findById(14L)).thenReturn(Optional.of(existing));

        assertThrows(
                com.truehunt.social.exception.UnauthorizedException.class,
                () -> postService.deletePost("14", "owner@example.com")
        );

        verify(postRepository, never()).delete(any(PostEntity.class));
    }
}
