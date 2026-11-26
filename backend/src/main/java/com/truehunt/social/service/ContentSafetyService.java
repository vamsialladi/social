package com.truehunt.social.service;

import com.truehunt.social.dto.ContentSafetyRequest;
import com.truehunt.social.dto.ContentSafetyResult;
import org.springframework.stereotype.Service;

@Service
public class ContentSafetyService {

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
}
