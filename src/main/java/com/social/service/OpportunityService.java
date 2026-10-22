package com.social.service;

import com.social.dto.request.CreateOpportunityRequest;
import com.social.dto.request.CreatePostRequest;
import com.social.dto.response.CursorPageResponse;
import com.social.dto.response.OpportunityResponse;

public interface OpportunityService {

    OpportunityResponse createOpportunity(String authorId, CreateOpportunityRequest request);

    CursorPageResponse<OpportunityResponse> getOpportunities(String cursor, int limit);

    OpportunityResponse createPost(String authorId, CreatePostRequest request);

    CursorPageResponse<OpportunityResponse> getOrdinaryPostsFeed(String cursor, int limit);

    OpportunityResponse getOpportunityById(String id);
}
