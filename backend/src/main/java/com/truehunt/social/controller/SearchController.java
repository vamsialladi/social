package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.model.NetworkProfileEntity;
import com.truehunt.social.model.PostEntity;
import com.truehunt.social.repository.NetworkProfileRepository;
import com.truehunt.social.repository.PostRepository;
import com.truehunt.social.repository.ProfileRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final PostRepository postRepository;
    private final NetworkProfileRepository networkProfileRepository;

    public SearchController(PostRepository postRepository, NetworkProfileRepository networkProfileRepository) {
        this.postRepository = postRepository;
        this.networkProfileRepository = networkProfileRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> search(@RequestParam(value = "q", defaultValue = "") String query) {
        String q = query.trim().toLowerCase();

        List<Map<String, Object>> matchedPeople = networkProfileRepository.findAll().stream()
                .filter(p -> q.isEmpty() ||
                        p.getName().toLowerCase().contains(q) ||
                        (p.getRole() != null && p.getRole().toLowerCase().contains(q)) ||
                        (p.getLocation() != null && p.getLocation().toLowerCase().contains(q)))
                .map(NetworkProfileEntity::toDto)
                .collect(Collectors.toList());

        List<Map<String, Object>> matchedPosts = postRepository.findAll().stream()
                .filter(p -> q.isEmpty() ||
                        (p.getContent() != null && p.getContent().toLowerCase().contains(q)) ||
                        (p.getTitle() != null && p.getTitle().toLowerCase().contains(q)) ||
                        (p.getAuthorName() != null && p.getAuthorName().toLowerCase().contains(q)))
                .map(PostEntity::toDto)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("query", query);
        result.put("people", matchedPeople);
        result.put("posts", matchedPosts);
        result.put("totalResults", matchedPeople.size() + matchedPosts.size());

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
