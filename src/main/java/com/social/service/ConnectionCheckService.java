package com.social.service;

public interface ConnectionCheckService {
    void validateCanStartConversation(String requesterId, String recipientId);
}
