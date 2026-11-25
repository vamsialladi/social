package com.truehunt.social.service;

import com.truehunt.social.model.NotificationEntity;
import com.truehunt.social.model.PostEntity;
import com.truehunt.social.model.PostReportEntity;
import com.truehunt.social.exception.ResourceNotFoundException;
import com.truehunt.social.repository.NotificationRepository;
import com.truehunt.social.repository.PostReportRepository;
import com.truehunt.social.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostServiceReportTest {

    @Test
    void storesReportAndNotifiesPostOwner() {
        PostRepository postRepository = mock(PostRepository.class);
        PostReportRepository postReportRepository = mock(PostReportRepository.class);
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        PostEntity post = new PostEntity();
        post.setAuthorId("owner@example.com");
        post.setAuthorName("Post Owner");
        post.setTitle("Community update");
        post.setContent("Post content");
        when(postRepository.findById(42L)).thenReturn(Optional.of(post));
        when(postReportRepository.save(any(PostReportEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PostService service = new PostService(
                postRepository,
                null,
                notificationRepository,
                null,
                null,
                postReportRepository,
                null,
                null,
                null);

        Map<String, Object> report = service.reportPost(
                "42",
                Map.of("reason", "Spam, scams or misleading content"),
                "reporter@example.com");

        assertEquals("42", report.get("postId"));
        assertEquals("Spam, scams or misleading content", report.get("reason"));

        ArgumentCaptor<NotificationEntity> notification = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notificationRepository).save(notification.capture());
        assertEquals("owner@example.com", notification.getValue().getRecipientId());
        assertEquals("REPORT", notification.getValue().getType());
        assertTrue(notification.getValue().getMessage().contains("reported"));
    }

    @Test
    void reporterCanRemoveTheirOwnReport() {
        PostRepository postRepository = mock(PostRepository.class);
        PostReportRepository postReportRepository = mock(PostReportRepository.class);
        PostReportEntity report = new PostReportEntity();
        report.setId(7L);
        report.setReporterEmail("reporter@example.com");
        when(postReportRepository.findById(7L)).thenReturn(Optional.of(report));

        PostService service = new PostService(
                postRepository,
                null,
                null,
                null,
                null,
                postReportRepository,
                null,
                null,
                null);

        Map<String, Object> response = service.deleteMyReport("7", "reporter@example.com");

        assertEquals(true, response.get("deleted"));
        verify(postReportRepository).delete(report);
    }

    @Test
    void reporterCannotRemoveAnotherUsersReport() {
        PostRepository postRepository = mock(PostRepository.class);
        PostReportRepository postReportRepository = mock(PostReportRepository.class);
        PostReportEntity report = new PostReportEntity();
        report.setId(7L);
        report.setReporterEmail("owner@example.com");
        when(postReportRepository.findById(7L)).thenReturn(Optional.of(report));

        PostService service = new PostService(
                postRepository,
                null,
                null,
                null,
                null,
                postReportRepository,
                null,
                null,
                null);

        org.junit.jupiter.api.Assertions.assertThrows(
                ResourceNotFoundException.class,
                () -> service.deleteMyReport("7", "reporter@example.com"));
        verify(postReportRepository, never()).delete(any(PostReportEntity.class));
    }
}
