package com.truehunt.social.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EntertainmentService {

    public List<Map<String, Object>> getAll() {
        List<Map<String, Object>> list = new ArrayList<>();

        Map<String, Object> e1 = new LinkedHashMap<>();
        e1.put("id", "ent-1");
        e1.put("name", "Mythri Movie Makers");
        e1.put("type", "Major Production House");
        e1.put("headquarters", "Hyderabad, Telangana");
        e1.put("location", "Hyderabad, Telangana");
        e1.put("recentProjects", List.of("Pushpa 2: The Rule", "Waltair Veerayya", "Srimanthudu"));
        e1.put("description", "One of Indian cinema's premier production banners behind massive theatrical pan-India spectacles and high-concept cinema.");
        e1.put("bannerImage", "https://images.unsplash.com/photo-1518173946687-a4c8892bbd9f?auto=format&fit=crop&w=1200&q=80");
        e1.put("banner", "https://images.unsplash.com/photo-1518173946687-a4c8892bbd9f?auto=format&fit=crop&w=1200&q=80");
        e1.put("logo", "https://images.unsplash.com/photo-1594909122845-11baa439b7bf?auto=format&fit=crop&w=200&q=80");
        e1.put("openRosters", 6);
        e1.put("activeProjects", 6);
        e1.put("openRoles", 14);
        e1.put("verified", true);
        list.add(e1);

        Map<String, Object> e2 = new LinkedHashMap<>();
        e2.put("id", "ent-2");
        e2.put("name", "Red Chillies VFX & Entertainment");
        e2.put("type", "Studio & VFX Facility");
        e2.put("headquarters", "Mumbai, Maharashtra");
        e2.put("location", "Mumbai, Maharashtra");
        e2.put("recentProjects", List.of("Jawan", "Dunki", "Zero"));
        e2.put("description", "Pioneering visual effects studio and content creation powerhouse driving next-generation digital filmmaking and pipeline innovation.");
        e2.put("bannerImage", "https://images.unsplash.com/photo-1536440136628-849c177e76a1?auto=format&fit=crop&w=1200&q=80");
        e2.put("banner", "https://images.unsplash.com/photo-1536440136628-849c177e76a1?auto=format&fit=crop&w=1200&q=80");
        e2.put("logo", "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=200&q=80");
        e2.put("openRosters", 12);
        e2.put("activeProjects", 5);
        e2.put("openRoles", 9);
        e2.put("verified", true);
        list.add(e2);

        Map<String, Object> e3 = new LinkedHashMap<>();
        e3.put("id", "ent-3");
        e3.put("name", "Hombale Films");
        e3.put("type", "Film Production Company");
        e3.put("headquarters", "Bengaluru, Karnataka");
        e3.put("location", "Bengaluru, Karnataka");
        e3.put("recentProjects", List.of("KGF Chapter 2", "Kantara", "Salaar"));
        e3.put("description", "Visionary cinema company rooted in indigenous storytelling that captured global box-office records and audience admiration.");
        e3.put("bannerImage", "https://images.unsplash.com/photo-1478720568477-152d9b164e26?auto=format&fit=crop&w=1200&q=80");
        e3.put("banner", "https://images.unsplash.com/photo-1478720568477-152d9b164e26?auto=format&fit=crop&w=1200&q=80");
        e3.put("logo", "https://images.unsplash.com/photo-1574717024653-61fd2cf4d44d?auto=format&fit=crop&w=200&q=80");
        e3.put("openRosters", 4);
        e3.put("activeProjects", 4);
        e3.put("openRoles", 11);
        e3.put("verified", true);
        list.add(e3);

        Map<String, Object> e4 = new LinkedHashMap<>();
        e4.put("id", "ent-4");
        e4.put("name", "Drishyam Films");
        e4.put("type", "Independent Film Studio");
        e4.put("headquarters", "Mumbai, Maharashtra");
        e4.put("location", "Mumbai, Maharashtra");
        e4.put("recentProjects", List.of("Masaan", "Newton", "Dhanak"));
        e4.put("description", "Champion of meaningful Indian independent cinema that represents original voices at Cannes, Sundance, Berlin, and worldwide festivals.");
        e4.put("bannerImage", "https://images.unsplash.com/photo-1524712245354-2c4e5e7121c0?auto=format&fit=crop&w=1200&q=80");
        e4.put("banner", "https://images.unsplash.com/photo-1524712245354-2c4e5e7121c0?auto=format&fit=crop&w=1200&q=80");
        e4.put("logo", "https://images.unsplash.com/photo-1507676184212-d03ab07a01bf?auto=format&fit=crop&w=200&q=80");
        e4.put("openRosters", 3);
        e4.put("activeProjects", 3);
        e4.put("openRoles", 5);
        e4.put("verified", true);
        list.add(e4);

        return list;
    }
}
