package com.social.service;

import com.social.dto.request.CreateConversationRequest;
import com.social.dto.response.ConversationResponse;
import com.social.dto.response.ConversationsPageResponse;
import com.social.entity.Conversation;

import java.util.List;

public interface ConversationService {

    List<ConversationResponse> getConversationsForUser(String userId);

    ConversationsPageResponse getConversationsForUser(String userId, String cursor, int limit);

    ConversationResponse getConversationById(String conversationId, String userId);

    ConversationResponse createOrGetConversation(String userId, CreateConversationRequest request);

    Conversation verifyAndGetConversation(String conversationId, String userId);
}
