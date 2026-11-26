package com.truehunt.social.service;

import com.truehunt.social.dto.ContentSafetyRequest;
import com.truehunt.social.dto.ContentSafetyResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentSafetyServiceTest {

    private final ContentSafetyService service = new ContentSafetyService();

    @Test
    void defaultTextContentIsSafe() {
        ContentSafetyResult result = service.checkText("Safe creative cinematic portfolio content.");
        assertTrue(result.isSafe());
    }

    @Test
    void nullContentIsSafe() {
        ContentSafetyResult result = service.checkContent(new ContentSafetyRequest(null));
        assertTrue(result.isSafe());
    }
}
