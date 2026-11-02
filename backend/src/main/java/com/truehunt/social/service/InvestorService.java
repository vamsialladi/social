package com.truehunt.social.service;

import com.truehunt.social.dto.InvestmentInterestRequest;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class InvestorService {

    private final List<Map<String, Object>> recordedInterests = Collections.synchronizedList(new ArrayList<>());

    public List<Map<String, Object>> getAll() {
        List<Map<String, Object>> list = new ArrayList<>();

        Map<String, Object> i1 = new LinkedHashMap<>();
        i1.put("id", "inv-1");
        i1.put("name", "Kalaari Cinema Ventures");
        i1.put("category", "Venture Slate Financier");
        i1.put("type", "Venture Slate Financier");
        i1.put("focusGenres", List.of("Pan-India Action", "Sci-Fi", "High-Concept Thriller"));
        i1.put("focus", List.of("Pan-India Action", "Sci-Fi", "High-Concept Thriller"));
        i1.put("ticketSize", "₹15 Cr — ₹60 Cr");
        i1.put("location", "Mumbai & Singapore");
        i1.put("description", "Institutional slate financing partner collaborating with verified production houses on multi-language commercial theatrical projects.");
        i1.put("criteria", "Packaged scripts with bankable director attached and minimum 40% co-financing pre-secured.");
        i1.put("verified", true);
        list.add(i1);

        Map<String, Object> i2 = new LinkedHashMap<>();
        i2.put("id", "inv-2");
        i2.put("name", "Deccan Angel Media Network");
        i2.put("category", "Angel Syndicate");
        i2.put("type", "Angel Syndicate");
        i2.put("focusGenres", List.of("Indie Festival Features", "Regional Language Dramas", "OTT Documentaries"));
        i2.put("focus", List.of("Indie Festival Features", "Regional Language Dramas", "OTT Documentaries"));
        i2.put("ticketSize", "₹1.5 Cr — ₹6 Cr");
        i2.put("location", "Hyderabad & Bengaluru");
        i2.put("description", "Syndicate of passionate entertainment angel investors backing fresh directorial voices with strong festival potential and streaming pre-sales.");
        i2.put("criteria", "Completed screenplays with detailed line budgets and experienced line producer on board.");
        i2.put("verified", true);
        list.add(i2);

        Map<String, Object> i3 = new LinkedHashMap<>();
        i3.put("id", "inv-3");
        i3.put("name", "CinemaBridge Capital");
        i3.put("category", "Bridge & Mezzanine Fund");
        i3.put("type", "Bridge & Mezzanine Fund");
        i3.put("focusGenres", List.of("Post-Production Completion", "VFX Financing", "Distribution Guarantee"));
        i3.put("focus", List.of("Post-Production Completion", "VFX Financing", "Distribution Guarantee"));
        i3.put("ticketSize", "₹3 Cr — ₹12 Cr");
        i3.put("location", "Chennai & London");
        i3.put("description", "Specialized mezzanine fund offering last-mile financing to bring completed principal photography through international post-production and worldwide festival distribution.");
        i3.put("criteria", "Rough cut or first assembly available for review by viewing committee.");
        i3.put("verified", true);
        list.add(i3);

        return list;
    }

    public Map<String, Object> expressInterest(InvestmentInterestRequest req) {
        Map<String, Object> record = new HashMap<>();
        record.put("id", "interest-" + UUID.randomUUID().toString().substring(0, 8));
        record.put("investorId", req.getInvestorId());
        record.put("investorName", req.getInvestorName());
        record.put("projectTitle", req.getProjectTitle());
        record.put("producerName", req.getProducerName());
        record.put("producerEmail", req.getProducerEmail());
        record.put("budgetRequested", req.getBudgetRequested());
        record.put("lookbookUrl", req.getLookbookUrl());
        record.put("synopsis", req.getSynopsis());
        record.put("submittedAt", new Date().toString());
        recordedInterests.add(record);

        Map<String, Object> res = new HashMap<>();
        res.put("interestId", record.get("id"));
        res.put("status", "SUBMITTED");
        res.put("message", "Pitch package submitted successfully to " + (req.getInvestorName() != null ? req.getInvestorName() : "investor"));
        return res;
    }
}
