package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.dto.JobDto;
import com.truehunt.social.dto.TalentDto;
import com.truehunt.social.model.NetworkProfileEntity;
import com.truehunt.social.model.PostEntity;
import com.truehunt.social.repository.NetworkProfileRepository;
import com.truehunt.social.repository.PostRepository;
import com.truehunt.social.service.JobService;
import com.truehunt.social.service.SettingsService;
import com.truehunt.social.service.TalentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final PostRepository postRepository;
    private final NetworkProfileRepository networkProfileRepository;
    private final TalentService talentService;
    private final JobService jobService;
    private final SettingsService settingsService;

    public SearchController(PostRepository postRepository,
                            NetworkProfileRepository networkProfileRepository,
                            TalentService talentService,
                            JobService jobService) {
        this(postRepository, networkProfileRepository, talentService, jobService, null);
    }

    @Autowired
    public SearchController(PostRepository postRepository,
                            NetworkProfileRepository networkProfileRepository,
                            TalentService talentService,
                            JobService jobService,
                            SettingsService settingsService) {
        this.postRepository = postRepository;
        this.networkProfileRepository = networkProfileRepository;
        this.talentService = talentService;
        this.jobService = jobService;
        this.settingsService = settingsService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> search(
            @RequestParam(value = "q", defaultValue = "") String query,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            org.springframework.security.core.Authentication authentication) {
        String caller = (userEmail != null && !userEmail.isBlank()) ? userEmail.trim()
                : (userId != null && !userId.isBlank()) ? userId.trim()
                : (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) ? authentication.getName() : null;
        String q = query.trim().toLowerCase();

        List<Map<String, Object>> matchedPeople = networkProfileRepository.findAll().stream()
                .filter(p -> settingsService == null || settingsService.isPublicProfileEnabled(p.getId()))
                .filter(p -> q.isEmpty() ||
                        p.getName().toLowerCase().contains(q) ||
                        (p.getRole() != null && p.getRole().toLowerCase().contains(q)) ||
                        (p.getLocation() != null && p.getLocation().toLowerCase().contains(q)))
                .map(NetworkProfileEntity::toDto)
                .collect(Collectors.toList());

        List<Map<String, Object>> matchedPosts = postRepository.findAll().stream()
                .filter(p -> !"private".equalsIgnoreCase(p.getAudience())
                        && !"connections".equalsIgnoreCase(p.getAudience()))
                .filter(p -> q.isEmpty() ||
                        (p.getContent() != null && p.getContent().toLowerCase().contains(q)) ||
                        (p.getTitle() != null && p.getTitle().toLowerCase().contains(q)) ||
                        (p.getAuthorName() != null && p.getAuthorName().toLowerCase().contains(q)))
                .map(p -> {
                    Map<String, Object> dto = p.toDto();
                    boolean isAuthor = caller != null && !caller.isBlank()
                            && p.getAuthorId() != null
                            && (p.getAuthorId().equalsIgnoreCase(caller)
                                    || (caller.equalsIgnoreCase("pro@truehunt.in") && "1000001".equalsIgnoreCase(p.getAuthorId()))
                                    || (caller.equalsIgnoreCase("1000001") && "pro@truehunt.in".equalsIgnoreCase(p.getAuthorId())));
                    dto.put("isAuthor", isAuthor);
                    if (settingsService != null) {
                        boolean likeCountEnabled = settingsService.isLikeCountEnabledForAuthor(p.getAuthorId());
                        boolean shareCountEnabled = settingsService.isShareCountEnabledForAuthor(p.getAuthorId());
                        if (isAuthor) {
                            dto.put("likesCount", p.getLikesCount());
                            dto.put("sharesCount", p.getSharesCount());
                            dto.put("showLikesCount", true);
                            dto.put("showSharesCount", true);
                            dto.put("likeCountDisabled", false);
                            dto.put("shareCountDisabled", false);
                        } else {
                            if (!likeCountEnabled) {
                                dto.put("likesCount", 0);
                                dto.put("showLikesCount", false);
                                dto.put("likeCountDisabled", true);
                            } else {
                                dto.put("showLikesCount", true);
                                dto.put("likeCountDisabled", false);
                            }
                            if (!shareCountEnabled) {
                                dto.put("sharesCount", 0);
                                dto.put("showSharesCount", false);
                                dto.put("shareCountDisabled", true);
                            } else {
                                dto.put("showSharesCount", true);
                                dto.put("shareCountDisabled", false);
                            }
                        }
                    }
                    return dto;
                })
                .collect(Collectors.toList());

        List<TalentDto> matchedTalents = talentService.search(q, null, null);
        List<JobDto> matchedJobs = jobService.getAll(Map.of("search", q));

        Map<String, Object> result = new HashMap<>();
        result.put("query", query);
        result.put("people", matchedPeople);
        result.put("posts", matchedPosts);
        result.put("talents", matchedTalents);
        result.put("jobs", matchedJobs);

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
