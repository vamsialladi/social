package com.truehunt.social.controller;

import com.truehunt.social.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class InvestorController {

    private final List<Map<String, Object>> investors = new ArrayList<>();

    public InvestorController() {
        initInvestors();
    }

    private void initInvestors() {
        Map<String, Object> i1 = new HashMap<>();
        i1.put("id", "inv-1");
        i1.put("name", "Kalaari Cinema Ventures");
        i1.put("category", "Venture Slate Financier");
        i1.put("focusGenres", List.of("Pan-India Action", "Sci-Fi", "High-Concept Thriller"));
        i1.put("ticketSize", "₹15 Cr — ₹60 Cr");
        i1.put("location", "Mumbai & Singapore");
        i1.put("description", "Institutional slate financing partner collaborating with verified production houses on multi-language commercial theatrical projects.");
        i1.put("criteria", "Packaged scripts with bankable director attached and minimum 40% co-financing pre-secured.");
        i1.put("verified", true);
        investors.add(i1);

        Map<String, Object> i2 = new HashMap<>();
        i2.put("id", "inv-2");
        i2.put("name", "Deccan Angel Media Network");
        i2.put("category", "Angel Syndicate");
        i2.put("focusGenres", List.of("Indie Festival Features", "Regional Language Dramas", "OTT Documentaries"));
        i2.put("ticketSize", "₹1.5 Cr — ₹6 Cr");
        i2.put("location", "Hyderabad & Bengaluru");
        i2.put("description", "Syndicate of passionate entertainment angel investors backing fresh directorial voices with strong festival potential and streaming pre-sales.");
        i2.put("criteria", "Completed screenplays with detailed line budgets and experienced line producer on board.");
        i2.put("verified", true);
        investors.add(i2);

        Map<String, Object> i3 = new HashMap<>();
        i3.put("id", "inv-3");
        i3.put("name", "CinemaBridge Capital");
        i3.put("category", "Co-Production & Debt Fund");
        i3.put("focusGenres", List.of("Pan-India Action", "Horror / Supernatural", "Youth Drama"));
        i3.put("ticketSize", "₹5 Cr — ₹25 Cr");
        i3.put("location", "Chennai & London");
        i3.put("description", "Structured bridge financing and completion guarantee partner for films with theatrical distribution locked.");
        i3.put("criteria", "Pre-sales minimum guarantee agreements and bonded completion bond from recognized entity.");
        i3.put("verified", true);
        investors.add(i3);
    }

    @GetMapping("/investors")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllInvestors() {
        return ResponseEntity.ok(ApiResponse.success(investors));
    }

    @PostMapping("/investment-interests")
    public ResponseEntity<ApiResponse<Map<String, Object>>> expressInterest(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Your pitch and investment expression of interest have been delivered to the investor's acquisitions desk.");
        res.put("interestId", "int-" + System.currentTimeMillis());
        res.put("submittedAt", new Date().toString());
        return ResponseEntity.ok(ApiResponse.success(res));
    }
}
