package com.social.service;

import com.social.dto.request.SendMessageRequest;
import com.social.dto.response.MessageResponse;
import com.social.dto.response.MessagesPageResponse;

public interface MessageService {

    MessagesPageResponse getMessages(String conversationId, String userId, String cursor, int limit);

    MessageResponse sendMessage(String conversationId, String senderId, SendMessageRequest request);

    int markAsRead(String conversationId, String userId);
}
