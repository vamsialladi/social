package com.truehunt.social.config;

import com.truehunt.social.model.*;
import com.truehunt.social.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(
            ProfileRepository profileRepository,
            NetworkProfileRepository networkProfileRepository,
            ConversationRepository conversationRepository,
            PostRepository postRepository,
            NotificationRepository notificationRepository,
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            // 0. Seed Demo Accounts matching frontend SignInPage.jsx and Mock Data
            if (userAccountRepository.findByEmail("aarav.varma@truehunt.in").isEmpty()) {
                UserAccountEntity aarav = new UserAccountEntity();
                aarav.setEmail("aarav.varma@truehunt.in");
                aarav.setPasswordHash(passwordEncoder.encode("TrueHunt2026!"));
                aarav.setName("Aarav Varma");
                aarav.setPhone("+91 9876543212");
                aarav.setRole("TALENT");
                aarav.setPublicUserId("1000002");
                aarav.setPrimaryCraft("Cinematography");
                aarav.setSubCraft("Director of Photography (DoP)");
                aarav.setExperience("8+ Years");
                aarav.setAcceptedTerms(true);
                aarav.setCreatedAt(LocalDateTime.now());
                userAccountRepository.save(aarav);
            }

            if (userAccountRepository.findByEmail("mythri.studios@truehunt.in").isEmpty()) {
                UserAccountEntity mythri = new UserAccountEntity();
                mythri.setEmail("mythri.studios@truehunt.in");
                mythri.setPasswordHash(passwordEncoder.encode("TrueHunt2026!"));
                mythri.setName("Mythri Movie Makers");
                mythri.setPhone("+91 9876543213");
                mythri.setRole("STUDIO");
                mythri.setPublicUserId("2000002");
                mythri.setCompanyName("Mythri Movie Makers");
                mythri.setHeadquarters("Jubilee Hills, Hyderabad");
                mythri.setPrimaryCraft("Production");
                mythri.setSubCraft("Feature Film Production House");
                mythri.setExperience("15+ Years");
                mythri.setAcceptedTerms(true);
                mythri.setCreatedAt(LocalDateTime.now());
                userAccountRepository.save(mythri);
            }

            if (userAccountRepository.findByEmail("pro@truehunt.in").isEmpty()) {
                UserAccountEntity talent = new UserAccountEntity();
                talent.setEmail("pro@truehunt.in");
                talent.setPasswordHash(passwordEncoder.encode("Password@123"));
                talent.setName("Cinema Professional");
                talent.setPhone("+91 9876543210");
                talent.setRole("TALENT");
                talent.setPublicUserId("1000001");
                talent.setPrimaryCraft("Acting");
                talent.setSubCraft("Lead Actor");
                talent.setExperience("5 Years");
                talent.setAcceptedTerms(true);
                talent.setCreatedAt(LocalDateTime.now());
                userAccountRepository.save(talent);
            }

            if (userAccountRepository.findByEmail("producer@truehunt.in").isEmpty()) {
                UserAccountEntity producer = new UserAccountEntity();
                producer.setEmail("producer@truehunt.in");
                producer.setPasswordHash(passwordEncoder.encode("Password@123"));
                producer.setName("Lead Producer");
                producer.setPhone("+91 9876543211");
                producer.setRole("PRODUCER");
                producer.setPublicUserId("2000001");
                producer.setPrimaryCraft("Producer");
                producer.setSubCraft("Executive Producer");
                producer.setExperience("10+ Years");
                producer.setAcceptedTerms(true);
                producer.setCreatedAt(LocalDateTime.now());
                userAccountRepository.save(producer);
            }

            // 1. Seed Profile with Experiences, Educations, and Projects
            if (profileRepository.findByUserId("me").isEmpty()) {
                ProfileEntity profile = new ProfileEntity();
                profile.setUserId("me");
                profile.setName("Chandrabose Alladi");
                profile.setPhone("+91 9876543210");
                profile.setEmail("pro@truehunt.in");
                profile.setProfessionalPhone("+91 9876543210");
                profile.setGender("Male");
                profile.setFacialHair("Beard");
                profile.setSkinTone("Fair");
                profile.setBodyBuild("Athletic");
                profile.setHeight("178");
                profile.setWeight("74");
                profile.setDateOfBirth("1996-08-15");
                profile.setExperience("5 Years");
                profile.setPrimaryCraft("Cast (Actors)");
                profile.setSubCraft("Lead Actor");
                profile.setHeadline("Lead Actor | Method Acting & Action Specialist");
                profile.setAddress("Hyderabad, Telangana, India");
                profile.setLanguages(List.of("Telugu", "English", "Hindi"));
                profile.setOtherLanguage("");
                profile.setTalents(List.of("Lead Actor", "Method Acting", "Action Stunts", "Voice Modulation"));
                profile.setWorkTypes(List.of("Cinema", "Web series", "Short films"));
                profile.setProfileImageUrl("https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=400&q=80");
                profile.setCoverImageUrl("https://images.unsplash.com/photo-1485846234645-a62644f84728?auto=format&fit=crop&w=1200&q=80");
                profile.setVideoUrls(List.of("https://www.youtube.com/watch?v=sample1"));
                profile.setHighlights("Cinema professional and lead actor dedicated to high-impact Indian cinema, disciplined on-set execution, and multi-lingual storytelling.");

                // Experiences
                Map<String, Object> exp1 = new HashMap<>();
                exp1.put("id", "exp-1");
                exp1.put("title", "Lead Actor");
                exp1.put("company", "Northstar Films");
                exp1.put("location", "Hyderabad, Telangana, India");
                exp1.put("startDate", "2023-01");
                exp1.put("endDate", "2024-06");
                exp1.put("isCurrent", false);
                exp1.put("description", "Portrayed the protagonist in a marquee Telugu drama feature film.");
                exp1.put("projectName", "Cinema Scope Production");
                exp1.put("organizationName", "Northstar Films");
                exp1.put("workedAs", "Lead Actor");
                profile.setExperiences(List.of(exp1));

                // Educations
                Map<String, Object> edu1 = new HashMap<>();
                edu1.put("id", "edu-1");
                edu1.put("institution", "Film and Television Institute of India (FTII)");
                edu1.put("degree", "Postgraduate Diploma");
                edu1.put("fieldOfStudy", "Direction and Screenplay Writing");
                edu1.put("startDate", "2019");
                edu1.put("endDate", "2022");
                edu1.put("grade", "First Class with Distinction");
                edu1.put("description", "Completed graduate project film screened at national student cinema showcases.");
                profile.setEducations(List.of(edu1));

                // Projects
                Map<String, Object> proj1 = new HashMap<>();
                proj1.put("id", "proj-1");
                proj1.put("title", "Echoes of the Deccan");
                proj1.put("technologies", "Arri Alexa Mini LF, Cooke Anamorphic/i, Avid Media Composer");
                proj1.put("projectUrl", "https://imdb.com/title/sample-truehunt");
                proj1.put("startDate", "2023");
                proj1.put("endDate", "2024");
                proj1.put("description", "Period feature short showcasing Deccan folklore. Selected for NFDC Film Bazaar screening.");
                profile.setProjects(List.of(proj1));

                profileRepository.save(profile);
            }

            if (profileRepository.findByUserId("1000004").isEmpty()) {
                ProfileEntity arjun = new ProfileEntity();
                arjun.setUserId("1000004");
                arjun.setName("Arjun Varma");
                arjun.setEmail("arjun.varma@truehunt.in");
                arjun.setPhone("+91 9876543299");
                arjun.setProfessionalPhone("+91 9876543299");
                arjun.setGender("Male");
                arjun.setFacialHair("Beard");
                arjun.setSkinTone("Medium");
                arjun.setBodyBuild("Athletic");
                arjun.setHeight("182");
                arjun.setWeight("78");
                arjun.setDateOfBirth("1995-04-12");
                arjun.setPrimaryCraft("Cast (Actors)");
                arjun.setSubCraft("Lead Actor");
                arjun.setHeadline("Lead Actor | Commercial & Indie Features | Telugu & Pan-India Cinema");
                arjun.setAddress("Hyderabad, Telangana, India");
                arjun.setExperience("7 Years");
                arjun.setHighlights("Dynamic lead actor specializing in intense romantic dramas, action thrillers, and commercial feature films.");
                arjun.setTalents(List.of("Lead Actor", "Method Acting", "Action Stunts", "Telugu & Hindi Diction"));
                arjun.setProfileImageUrl("https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=400&q=80");
                profileRepository.save(arjun);
            }

            if (profileRepository.findByUserId("1000003").isEmpty()) {
                ProfileEntity meera = new ProfileEntity();
                meera.setUserId("1000003");
                meera.setName("Meera Nambiar");
                meera.setEmail("meera.nambiar@truehunt.in");
                meera.setPhone("+91 9876543298");
                meera.setProfessionalPhone("+91 9876543298");
                meera.setGender("Female");
                meera.setFacialHair("");
                meera.setSkinTone("Fair");
                meera.setBodyBuild("Slim");
                meera.setHeight("165");
                meera.setWeight("54");
                meera.setDateOfBirth("1998-11-20");
                meera.setPrimaryCraft("Cast (Actors)");
                meera.setSubCraft("Lead Actress");
                meera.setHeadline("Lead Actress & Theatre Artist | NSD Alumna | Multilingual Performer");
                meera.setAddress("Kochi & Bengaluru, India");
                meera.setExperience("5 Years");
                meera.setHighlights("Method actress trained at National School of Drama. Experienced in dramatic leads and intense character roles.");
                meera.setTalents(List.of("Lead Actress", "Method Acting", "Classical Dance", "Voice Modulation"));
                meera.setProfileImageUrl("https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=400&q=80");
                profileRepository.save(meera);
            }

            if (profileRepository.findByUserId("1000005").isEmpty()) {
                ProfileEntity rohan = new ProfileEntity();
                rohan.setUserId("1000005");
                rohan.setName("Rohan Vats");
                rohan.setEmail("rohan.vats@truehunt.in");
                rohan.setPhone("+91 9876543297");
                rohan.setPrimaryCraft("Cast (Actors)");
                rohan.setSubCraft("Character Actor");
                rohan.setHeadline("Character Actor & Antagonist | Hindi & Regional Cinema");
                rohan.setAddress("Mumbai, Maharashtra, India");
                rohan.setExperience("10 Years");
                rohan.setHighlights("Versatile character actor and antagonist with extensive theatre and feature film background.");
                rohan.setTalents(List.of("Character Actor", "Antagonist / Villain", "Improvisation", "Voice Modulation"));
                rohan.setProfileImageUrl("https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=400&q=80");
                profileRepository.save(rohan);
            }

            // 2. Seed Network Suggested Profiles
            if (networkProfileRepository.count() == 0) {
                networkProfileRepository.save(new NetworkProfileEntity(
                        "maya", "Maya Krishnan", "MK", "active",
                        "Actor · Voice artist", "Hyderabad, Telangana",
                        "12 mutual connections", "network-avatar-coral"
                ));
                networkProfileRepository.save(new NetworkProfileEntity(
                        "rohan", "Rohan Mehta", "RM", "offline",
                        "Cinematographer · Editor", "Mumbai, Maharashtra",
                        "8 mutual connections", "network-avatar-blue"
                ));
                networkProfileRepository.save(new NetworkProfileEntity(
                        "aarav", "Aarav Varma", "AV", "active",
                        "Cinematographer (DoP)", "Mumbai, India",
                        "15 mutual connections", "network-avatar-green"
                ));
            }

            // 3. Seed Conversations (Matching Messages.jsx)
            if (conversationRepository.count() == 0) {
                ConversationEntity c1 = new ConversationEntity();
                c1.setName("Akshay Chamola");
                c1.setInitials("AC");
                c1.setColor("blue");
                c1.setPreview("Unionpoint is adding a Chapter Support Associate...");
                c1.setDate("Oct 19");
                c1.setCategory("Opportunities");
                c1.setUnread(true);
                c1.setSponsored(true);
                c1.setSubject("Unionpoint is adding a Chapter Support Associate");
                c1.setDescription("Make an impact with a team that is building stronger communities.");
                c1.setMailbox("Focused");
                c1.setThread(List.of(
                        Map.of("id", "akshay-1", "content", "Hi Chandrabose, thanks for your interest in this opportunity.", "direction", "incoming"),
                        Map.of("id", "akshay-2", "content", "Thanks for reaching out. Could you share more details?", "direction", "outgoing"),
                        Map.of("id", "akshay-3", "content", "Absolutely! I would be happy to tell you more.", "direction", "incoming")
                ));
                conversationRepository.save(c1);

                ConversationEntity c2 = new ConversationEntity();
                c2.setName("Priya Sharma");
                c2.setInitials("PS");
                c2.setColor("rose");
                c2.setPreview("Thanks for sharing your profile. Let’s connect soon!");
                c2.setDate("Oct 11");
                c2.setCategory("Connections");
                c2.setUnread(true);
                c2.setSponsored(false);
                c2.setSubject("Great connecting with you");
                c2.setDescription("Thanks for sharing your profile. Let’s connect soon!");
                c2.setMailbox("Focused");
                c2.setThread(List.of(
                        Map.of("id", "priya-1", "content", "Thanks for sharing your profile. Let’s connect soon!", "direction", "incoming")
                ));
                conversationRepository.save(c2);
            }

            // 4. Seed sample Posts / Opportunities if empty
            if (postRepository.count() == 0) {
                PostEntity p1 = new PostEntity();
                p1.setAuthorId("me");
                p1.setAuthorName("Chandrabose Alladi");
                p1.setAuthorHeadline("Acting · Direction · Screenwriting");
                p1.setType("post");
                p1.setContent("Excited to announce the start of casting auditions for our new upcoming short film series in Hyderabad! Looking forward to collaborating with talented cinematographers and actors.");
                p1.setCreatedAt(LocalDateTime.now().minusDays(1));
                postRepository.save(p1);

                PostEntity opp1 = new PostEntity();
                opp1.setAuthorId("me");
                opp1.setAuthorName("TrueHunt Talent Scouts");
                opp1.setAuthorHeadline("Film & Media Production");
                opp1.setType("opportunity");
                opp1.setContent("Casting Call: Lead and Supporting roles needed for feature film project shooting in Vizag & Hyderabad. Apply with your video showreel!");
                opp1.setCreatedAt(LocalDateTime.now().minusHours(4));
                postRepository.save(opp1);
            }

            // 5. Seed sample Endorsement/Notification
            if (notificationRepository.count() == 0) {
                notificationRepository.save(new NotificationEntity(
                        "me", "Endorsement Received", "Maya Krishnan endorsed your talent for Direction and Screenwriting.", "endorsement"
                ));
            }
        };
    }
}
