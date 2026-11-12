package com.truehunt.social.service;

import com.truehunt.social.dto.SupportTicketRequest;
import com.truehunt.social.model.SupportTicketEntity;
import com.truehunt.social.repository.SupportTicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class SupportService {

    private final List<Map<String, Object>> tickets = Collections.synchronizedList(new ArrayList<>());
    private final SupportTicketRepository supportTicketRepository;

    public SupportService() {
        this(null);
    }

    @Autowired
    public SupportService(SupportTicketRepository supportTicketRepository) {
        this.supportTicketRepository = supportTicketRepository;
    }

    public Map<String, Object> submitTicket(SupportTicketRequest req) {
        String id = "ticket-" + UUID.randomUUID().toString().substring(0, 8);
        if (supportTicketRepository != null) {
            SupportTicketEntity entity = new SupportTicketEntity();
            entity.setId(id);
            entity.setName(req.getName());
            entity.setEmail(req.getEmail());
            entity.setSubject(req.getSubject());
            entity.setCategory(req.getCategory());
            entity.setMessage(req.getMessage());
            entity.setPriority(req.getPriority());
            entity.setStatus("OPEN");
            entity.setCreatedAt(LocalDateTime.now());
            supportTicketRepository.save(entity);
        }

        Map<String, Object> ticket = new HashMap<>();
        ticket.put("id", id);
        ticket.put("name", req.getName());
        ticket.put("email", req.getEmail());
        ticket.put("subject", req.getSubject());
        ticket.put("category", req.getCategory());
        ticket.put("message", req.getMessage());
        ticket.put("priority", req.getPriority());
        ticket.put("status", "OPEN");
        ticket.put("submittedAt", new Date().toString());
        tickets.add(ticket);

        Map<String, Object> res = new HashMap<>();
        res.put("ticketId", id);
        res.put("message", "Support ticket created successfully. Reference ID: " + id);
        res.put("status", "OPEN");
        return res;
    }

    public List<Map<String, Object>> getFaqs() {
        return List.of(
                Map.of(
                        "q", "What is TrueHunt and how does it benefit film professionals?",
                        "a", "TrueHunt (truehunt.in) is a dedicated professional networking and opportunity platform tailored exclusively for the entertainment, film, and media ecosystem. It connects actors, directors, technicians, writers, production houses, and film investors on a professional platform designed for verified film portfolios rather than superficial social media metrics.",
                        "question", "What is TrueHunt and how does it benefit film professionals?",
                        "answer", "TrueHunt (truehunt.in) is a dedicated professional networking and opportunity platform tailored exclusively for the entertainment, film, and media ecosystem.",
                        "category", "Platform & Mission"
                ),
                Map.of(
                        "q", "How does TrueHunt ensure privacy and protection of contact information?",
                        "a", "TrueHunt enforces strict privacy controls. Personal phone numbers and private email addresses are never exposed publicly. Filmmakers connect and converse through TrueHunt verified messaging and application channels, ensuring you maintain complete control over who contacts you.",
                        "question", "How does TrueHunt ensure privacy and protection of contact information?",
                        "answer", "TrueHunt enforces strict privacy controls. Personal phone numbers and private email addresses are never exposed publicly.",
                        "category", "Privacy & Safety"
                ),
                Map.of(
                        "q", "How can casting directors and production houses post auditions or crew requirements?",
                        "a", "Verified production houses and recruiters can post project roles on the Jobs board, specifying character breakdowns, experience levels, budget guidelines, and deadlines. Applications are received with filtered showreels and credits for swift review.",
                        "question", "How can casting directors and production houses post auditions or crew requirements?",
                        "answer", "Verified production houses and recruiters can post project roles on the Jobs board, specifying character breakdowns, experience levels, budget guidelines, and deadlines.",
                        "category", "For Producers & Studios"
                ),
                Map.of(
                        "q", "Does TrueHunt take commission or guarantee film funding on Investors Connect?",
                        "a", "No. TrueHunt is a professional discovery directory and networking facilitator. We do not act as an investment broker, do not promise financial returns, and do not charge commissions on funding deals. All financing discussions are strictly between independent creators and authorized investors.",
                        "question", "Does TrueHunt take commission or guarantee film funding on Investors Connect?",
                        "answer", "No. TrueHunt is a professional discovery directory and networking facilitator without platform commissions.",
                        "category", "For Investors"
                ),
                Map.of(
                        "q", "What is the LCM App mentioned on the platform?",
                        "a", "The LCM App (Location, Crew & Movie Management) is TrueHunt’s companion production management tool designed for on-set logistics, digital call sheets, location scout clearances, and production reporting.",
                        "question", "What is the LCM App mentioned on the platform?",
                        "answer", "The LCM App is TrueHunt’s companion production management tool designed for on-set logistics, digital call sheets, and clearances.",
                        "category", "Production Tools"
                ),
                Map.of(
                        "q", "Is TrueHunt free for aspiring artists and technicians?",
                        "a", "Yes, creating a professional talent profile, showcasing credits and showreels, and discovering public industry opportunities is free. Advanced studio management tools and verified casting workflows will offer dedicated tiers.",
                        "question", "Is TrueHunt free for aspiring artists and technicians?",
                        "answer", "Yes, creating a professional talent profile and discovering public industry opportunities is free.",
                        "category", "For Talent & Crew"
                )
        );
    }
}
