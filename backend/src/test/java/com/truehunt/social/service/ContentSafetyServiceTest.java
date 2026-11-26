package com.truehunt.social.service;

import com.truehunt.social.dto.ContentSafetyRequest;
import com.truehunt.social.dto.ContentSafetyResult;
import com.truehunt.social.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ContentSafetyServiceTest {

    @Test
    void defaultTextContentIsSafe() {
        ContentSafetyResult result = createService("https://safety.example", "test-key", 4)
                .service()
                .checkText("Safe creative cinematic portfolio content.");

        assertTrue(result.isSafe());
    }

    @Test
    void nullContentIsSafe() {
        ContentSafetyResult result = createService("https://safety.example", "test-key", 4)
                .service()
                .checkContent(new ContentSafetyRequest(null));

        assertTrue(result.isSafe());
    }

    @Test
    void rejectsAdultImagesAtOrAboveConfiguredSeverity() {
        SafetyClient client = createService("https://safety.example/", "test-key", 4);
        client.server().expect(requestTo("https://safety.example/contentsafety/image:analyze?api-version=2024-09-01"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Ocp-Apim-Subscription-Key", "test-key"))
                .andExpect(content().json(
                        "{\"image\":{\"content\":\"AQID\"},\"categories\":[\"Sexual\"],\"outputType\":\"FourSeverityLevels\"}"
                ))
                .andRespond(withSuccess(
                        "{\"categoriesAnalysis\":[{\"category\":\"Sexual\",\"severity\":4}]}",
                        MediaType.APPLICATION_JSON
                ));

        ContentSafetyResult result = client.service().checkImage(new byte[]{1, 2, 3});

        assertFalse(result.isSafe());
        client.server().verify();
    }

    @Test
    void acceptsImagesBelowConfiguredSeverity() {
        SafetyClient client = createService("https://safety.example", "test-key", 4);
        client.server().expect(requestTo("https://safety.example/contentsafety/image:analyze?api-version=2024-09-01"))
                .andRespond(withSuccess(
                        "{\"categoriesAnalysis\":[{\"category\":\"Sexual\",\"severity\":2}]}",
                        MediaType.APPLICATION_JSON
                ));

        assertTrue(client.service().checkImage(new byte[]{1}).isSafe());
        client.server().verify();
    }

    @Test
    void failsClosedWhenImageSafetyIsNotConfigured() {
        ContentSafetyService service = createService("", "", 4).service();

        BadRequestException error = assertThrows(
                BadRequestException.class,
                () -> service.checkImage(new byte[]{1})
        );

        assertTrue(error.getMessage().contains("not configured"));
    }

    private SafetyClient createService(String endpoint, String key, int blockSeverity) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        return new SafetyClient(
                new ContentSafetyService(builder, endpoint, key, blockSeverity),
                server
        );
    }

    private record SafetyClient(ContentSafetyService service, MockRestServiceServer server) {}
}
