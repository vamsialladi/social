package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.dto.TalentDto;
import com.truehunt.social.service.TalentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/talents")
public class TalentController {

    private final TalentService talentService;

    public TalentController(TalentService talentService) {
        this.talentService = talentService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TalentDto>>> getAllTalents(
            @RequestParam(required = false) Map<String, String> params
    ) {
        List<TalentDto> list = talentService.getAll(params);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TalentDto>> getTalentById(@PathVariable String id) {
        TalentDto talent = talentService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(talent));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<TalentDto>>> searchTalents(
            @RequestParam(required = false) Map<String, String> params
    ) {
        List<TalentDto> list = talentService.getAll(params);
        return ResponseEntity.ok(ApiResponse.success(list));
    }
}
