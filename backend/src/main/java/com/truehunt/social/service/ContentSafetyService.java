package com.truehunt.social.service;

import com.truehunt.social.dto.ContentSafetyRequest;
import com.truehunt.social.dto.ContentSafetyResult;
import com.truehunt.social.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class ContentSafetyService {

    private static final long MAX_IMAGE_SIZE_BYTES = 4L * 1024 * 1024;
    private static final String IMAGE_ANALYZE_PATH =
            "/contentsafety/image:analyze?api-version=2024-09-01";

    private final RestClient restClient;
    private final String endpoint;
    private final String key;
    private final int blockSeverity;

    public ContentSafetyService(
            RestClient.Builder restClientBuilder,
            @Value("${app.content-safety.endpoint:}") String endpoint,
            @Value("${app.content-safety.key:}") String key,
            @Value("${app.content-safety.block-severity:4}") int blockSeverity
    ) {
        this.restClient = restClientBuilder.build();
        this.endpoint = endpoint;
        this.key = key;
        this.blockSeverity = blockSeverity;
    }

    public ContentSafetyResult checkText(String text) {
        if (text == null || text.isBlank()) {
            return ContentSafetyResult.safe();
        }
        return ContentSafetyResult.safe();
    }

    public ContentSafetyResult checkContent(ContentSafetyRequest request) {
        if (request == null) {
            return ContentSafetyResult.safe();
        }
        return checkText(request.getText());
    }

    public ContentSafetyResult checkImage(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw scanFailed("The selected image is empty.");
        }
        if (imageBytes.length > MAX_IMAGE_SIZE_BYTES) {
            throw scanFailed("Images must be no larger than 4 MB to complete the safety check.");
        }
        if (endpoint == null || endpoint.isBlank() || key == null || key.isBlank()) {
            throw scanFailed("Image safety scanning is not configured. Please try again later.");
        }

        String baseEndpoint = endpoint.endsWith("/")
                ? endpoint.substring(0, endpoint.length() - 1)
                : endpoint;
        Map<String, Object> requestBody = Map.of(
                "image", Map.of("content", Base64.getEncoder().encodeToString(imageBytes)),
                "categories", List.of("Sexual"),
                "outputType", "FourSeverityLevels"
        );

        try {
            Map<String, Object> response = restClient.post()
                    .uri(baseEndpoint + IMAGE_ANALYZE_PATH)
                    .header("Ocp-Apim-Subscription-Key", key)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            int severity = getSexualSeverity(response);
            if (severity >= blockSeverity) {
                return ContentSafetyResult.rejected(
                        "This image contains adult content and cannot be uploaded."
                );
            }
            return ContentSafetyResult.safe();
        } catch (RestClientException | IllegalArgumentException exception) {
            throw scanFailed("The image could not be checked for adult content. Please try again.");
        }
    }

    private int getSexualSeverity(Map<String, Object> response) {
        Object analyses = response != null ? response.get("categoriesAnalysis") : null;
        if (!(analyses instanceof List<?> categories)) {
            throw scanFailed("The image safety scanner returned an invalid response.");
        }

        for (Object value : categories) {
            if (!(value instanceof Map<?, ?> category)
                    || !"Sexual".equalsIgnoreCase(String.valueOf(category.get("category")))) {
                continue;
            }
            Object severity = category.get("severity");
            if (severity instanceof Number number) {
                return number.intValue();
            }
            throw scanFailed("The image safety scanner returned an invalid response.");
        }
        throw scanFailed("The image safety scanner did not return an adult-content result.");
    }

    private BadRequestException scanFailed(String message) {
        return new BadRequestException("MEDIA_SCAN_FAILED", message);
    }
}
