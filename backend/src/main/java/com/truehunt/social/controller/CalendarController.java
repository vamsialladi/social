package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.dto.CalendarEntryCompletionRequest;
import com.truehunt.social.dto.CalendarEntryRequest;
import com.truehunt.social.security.AuthenticatedUserResolver;
import com.truehunt.social.service.CalendarService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/calendar/entries")
public class CalendarController {

    private final CalendarService calendarService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public CalendarController(
            CalendarService calendarService,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.calendarService = calendarService;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getEntries(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                calendarService.getEntries(authenticatedUserResolver.requireJwtEmail(request))
        ));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> saveEntry(
            @Valid @RequestBody CalendarEntryRequest entry,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                calendarService.saveEntry(authenticatedUserResolver.requireJwtEmail(request), entry)
        ));
    }

    @PatchMapping("/{entryId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateCompletion(
            @PathVariable String entryId,
            @Valid @RequestBody CalendarEntryCompletionRequest requestBody,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                calendarService.updateCompletion(
                        authenticatedUserResolver.requireJwtEmail(request),
                        entryId,
                        requestBody.getCompleted()
                )
        ));
    }

    @DeleteMapping("/{entryId}")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteEntry(
            @PathVariable String entryId,
            HttpServletRequest request
    ) {
        calendarService.deleteEntry(authenticatedUserResolver.requireJwtEmail(request), entryId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Calendar entry deleted", "id", entryId)));
    }
}
