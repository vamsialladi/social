package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import com.truehunt.social.dto.InvestmentInterestRequest;
import com.truehunt.social.service.InvestorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class InvestorController {

    private final InvestorService investorService;

    public InvestorController(InvestorService investorService) {
        this.investorService = investorService;
    }

    @GetMapping("/investors")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllInvestors() {
        return ResponseEntity.ok(ApiResponse.success(investorService.getAll()));
    }

    @PostMapping("/investment-interests")
    public ResponseEntity<ApiResponse<Map<String, Object>>> expressInterest(@Valid @RequestBody InvestmentInterestRequest req) {
        return ResponseEntity.ok(ApiResponse.success(investorService.expressInterest(req)));
    }
}
