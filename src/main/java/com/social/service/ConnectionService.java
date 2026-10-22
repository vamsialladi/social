package com.social.service;

import com.social.dto.request.SendConnectionRequest;
import com.social.dto.request.UpdateConnectionStatusRequest;
import com.social.dto.response.ConnectionResponse;
import com.social.dto.response.UserProfileResponse;

import java.util.List;

public interface ConnectionService {

    List<UserProfileResponse> getSuggestions(String currentUserId, int limit);

    ConnectionResponse sendConnectionRequest(String currentUserId, SendConnectionRequest request);

    ConnectionResponse updateConnectionStatus(String currentUserId, String connectionId, UpdateConnectionStatusRequest request);

    List<ConnectionResponse> getAcceptedConnections(String currentUserId);

    ConnectionResponse getConnectionById(String currentUserId, String connectionId);
}
