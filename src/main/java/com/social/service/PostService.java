package com.social.service;

import com.social.dto.request.PostCreateRequest;
import com.social.dto.request.PostReportRequest;
import com.social.dto.request.PostUpdateRequest;
import com.social.dto.response.CursorPageResponse;
import com.social.dto.response.PostResponse;

public interface PostService {
    CursorPageResponse<PostResponse> getFeed(String cursor, int limit, String viewerUserId);
    PostResponse getPostById(String postId, String viewerUserId);
    PostResponse createPost(PostCreateRequest request, String authorId);
    PostResponse updatePost(String postId, PostUpdateRequest request, String currentUserId);
    void deletePost(String postId, String currentUserId);
    void reportPost(String postId, PostReportRequest request, String reporterUserId);
}
