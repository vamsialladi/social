package com.truehunt.social.controller;

import com.truehunt.social.service.InvestorAccessService;
import com.truehunt.social.service.PostService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostControllerCastingAccessTest {

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void anonymousCastingProfileListIsEmptyWithoutAnUnauthorizedResponse() {
        PostService postService = mock(PostService.class);
        PostController controller = new PostController(
                postService,
                mock(InvestorAccessService.class)
        );

        assertEquals(
                List.of(),
                controller.getPosts("casting", null, null).getBody().getData()
        );
        verify(postService, never()).getPosts("casting", null);
    }

    @Test
    void authenticatedUsersCanRequestCastingProfiles() {
        PostService postService = mock(PostService.class);
        PostController controller = new PostController(postService, mock(InvestorAccessService.class));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("viewer@example.com", "token", List.of())
        );
        when(postService.getPosts("casting", "viewer@example.com")).thenReturn(List.of());

        assertEquals(
                List.of(),
                controller.getPosts("casting", null, null).getBody().getData()
        );
    }
}
