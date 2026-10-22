package com.social.service.impl;

import com.social.service.ConnectionCheckService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ConnectionCheckServiceImpl implements ConnectionCheckService {

    private static final Logger log = LoggerFactory.getLogger(ConnectionCheckServiceImpl.class);
    private final boolean requireAcceptedConnection;

    public ConnectionCheckServiceImpl(
            @Value("${app.messaging.require-accepted-connection:false}") boolean requireAcceptedConnection) {
        this.requireAcceptedConnection = requireAcceptedConnection;
    }

    @Override
    public void validateCanStartConversation(String requesterId, String recipientId) {
        if (requesterId.equals(recipientId)) {
            throw new IllegalArgumentException("Cannot start a conversation with yourself");
        }

        if (requireAcceptedConnection) {
            log.info("Checking accepted connection requirement between {} and {}", requesterId, recipientId);
        }
    }
}
