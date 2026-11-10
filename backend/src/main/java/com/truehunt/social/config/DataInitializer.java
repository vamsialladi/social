package com.truehunt.social.config;

import com.truehunt.social.model.*;
import com.truehunt.social.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
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
            UserProfileRelationshipRepository relationshipRepository,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate
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
                producer.setAcceptedTerms(true);
                producer.setCreatedAt(LocalDateTime.now());
                userAccountRepository.save(producer);
            }

            // 1. Seed the demo profile with education and project details
            if (profileRepository.findByUserId("me").isEmpty()) {
                ProfileEntity profile = new ProfileEntity();
                profile.setUserId("me");
                profile.setName("Chandrabose Alladi");
                profile.setPhone("+91 9876543210");
                profile.setProfessionalPhone("+91 9876543210");
                profile.setEmail("pro@truehunt.in");
                profile.setPrimaryCraft("Cast (Actors)");
                profile.setSubCraft("Lead Actor");
                profile.setHeadline("Lead Actor | Method Acting & Action Specialist");
                profile.setAddress("Hyderabad, Telangana, India");
                profile.setFacialHair("Clean Shaven");
                profile.setOtherLanguage("");
                profile.setTalents(List.of("Lead Actor", "Method Acting", "Action Stunts", "Voice Modulation"));
                profile.setProfileImageUrl("https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=400&q=80");
                profile.setCoverImageUrl("https://images.unsplash.com/photo-1485846234645-a62644f84728?auto=format&fit=crop&w=1200&q=80");
                profile.setVideoUrls(List.of("https://www.youtube.com/watch?v=sample1"));
                profile.setHighlights("Cinema professional and lead actor dedicated to high-impact Indian cinema, disciplined on-set execution, and multi-lingual storytelling.");
                profile.setBio(profile.getHighlights());

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
                arjun.setPrimaryCraft("Cast (Actors)");
                arjun.setSubCraft("Lead Actor");
                arjun.setHeadline("Lead Actor | Commercial & Indie Features | Telugu & Pan-India Cinema");
                arjun.setAddress("Hyderabad, Telangana, India");
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
                meera.setPrimaryCraft("Cast (Actors)");
                meera.setSubCraft("Lead Actress");
                meera.setHeadline("Lead Actress & Theatre Artist | NSD Alumna | Multilingual Performer");
                meera.setAddress("Kochi & Bengaluru, India");
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
                rohan.setHighlights("Versatile character actor and antagonist with extensive theatre and feature film background.");
                rohan.setTalents(List.of("Character Actor", "Antagonist / Villain", "Improvisation", "Voice Modulation"));
                rohan.setProfileImageUrl("https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=400&q=80");
                profileRepository.save(rohan);
            }

            clearLegacyDefaultProfileDetails(profileRepository, userAccountRepository, jdbcTemplate);

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

            // 6. Clean up malformed relationship records from historical invalid connect clicks
            for (UserProfileRelationshipEntity rel : relationshipRepository.findAll()) {
                String target = rel.getTargetProfileId();
                String owner = rel.getOwnerUserId();
                if ((target != null && (target.contains(".jpg") || target.contains(".png") || target.contains("uploads/") || target.isBlank()))
                        || (owner != null && (owner.contains(".jpg") || owner.contains(".png") || owner.contains("uploads/") || owner.isBlank()))) {
                    relationshipRepository.delete(rel);
                }
            }

            // Sanitize conversation thread directions and receiver IDs
            conversationRepository.findAll().forEach(conv -> {
                String u1 = conv.getUser1Id();
                String u2 = conv.getUser2Id();
                if (u1 != null && u2 != null) {
                    List<Map<String, Object>> thread = conv.getThread();
                    boolean modified = false;
                    for (Map<String, Object> m : thread) {
                        String sId = (String) m.get("senderId");
                        String rId = (String) m.get("receiverId");
                        if (sId != null && (rId == null || rId.equalsIgnoreCase(sId))) {
                            String correctReceiver = sId.equalsIgnoreCase(u1) ? u2 : u1;
                            m.put("receiverId", correctReceiver);
                            modified = true;
                        }
                    }
                    if (modified) {
                        conv.setThread(thread);
                        conversationRepository.save(conv);
                    }
                }
            });
        };
    }

    private void clearLegacyDefaultProfileDetails(
            ProfileRepository profileRepository,
            UserAccountRepository userAccountRepository,
            JdbcTemplate jdbcTemplate
    ) {
        String migrationKey = "clear-generated-profile-defaults-v2";
        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS app_data_migrations "
                        + "(migration_key VARCHAR(128) PRIMARY KEY)"
        );
        Integer completed = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_data_migrations WHERE migration_key = ?",
                Integer.class,
                migrationKey
        );
        if (completed != null && completed > 0) {
            return;
        }

        clearAccountExperience(userAccountRepository, "aarav.varma@truehunt.in", "8+ Years");
        clearAccountExperience(userAccountRepository, "mythri.studios@truehunt.in", "15+ Years");
        clearAccountExperience(userAccountRepository, "pro@truehunt.in", "5 Years");
        clearAccountExperience(userAccountRepository, "producer@truehunt.in", "10+ Years");

        clearProfileExperience(profileRepository, "me", "5 Years");
        clearProfileExperience(profileRepository, "1000001", "5 Years");
        clearProfileExperience(profileRepository, "1000002", "8+ Years");
        clearProfileExperience(profileRepository, "2000001", "10+ Years");
        clearProfileExperience(profileRepository, "2000002", "15+ Years");
        clearProfileExperience(profileRepository, "1000003", "5 Years");
        clearProfileExperience(profileRepository, "1000004", "7 Years");
        clearProfileExperience(profileRepository, "1000005", "10 Years");

        Set<String> generatedSignupEmails = new HashSet<>();
        for (ProfileEntity profile : profileRepository.findAll()) {
            List<Map<String, Object>> experiences = profile.getExperiences();
            boolean hasGeneratedSignupExperience = experiences.stream()
                    .anyMatch(this::isGeneratedSignupExperience);
            boolean changed = false;
            if ("me".equals(profile.getUserId())) {
                if (profile.getLanguages().equals(List.of("Telugu", "English", "Hindi"))) {
                    profile.setLanguages(List.of());
                    changed = true;
                }
                if (profile.getWorkTypes().equals(List.of("Cinema", "Web series", "Short films"))) {
                    profile.setWorkTypes(List.of());
                    changed = true;
                }
            }
            if (hasGeneratedSignupExperience) {
                if (profile.getEmail() != null) {
                    generatedSignupEmails.add(profile.getEmail());
                }
                if (profile.getLanguages().equals(List.of("English", "Telugu"))) {
                    profile.setLanguages(List.of());
                    changed = true;
                }
                if (profile.getWorkTypes().equals(List.of("Cinema"))) {
                    profile.setWorkTypes(List.of());
                    changed = true;
                }
            }

            List<Map<String, Object>> userAddedExperiences = experiences.stream()
                    .filter(experience -> !isGeneratedSignupExperience(experience))
                    .toList();
            if (userAddedExperiences.size() != experiences.size()) {
                profile.setExperiences(userAddedExperiences);
                changed = true;
            }
            if (hasGeneratedSignupExperience && "3 - 5 Years".equals(profile.getExperience())) {
                profile.setExperience(null);
                changed = true;
            }
            if (changed) {
                profileRepository.save(profile);
            }
        }

        for (UserAccountEntity account : userAccountRepository.findAll()) {
            if (generatedSignupEmails.contains(account.getEmail())
                    && "3 - 5 Years".equals(account.getExperience())) {
                account.setExperience(null);
                userAccountRepository.save(account);
            }
        }
        jdbcTemplate.update(
                "INSERT INTO app_data_migrations (migration_key) VALUES (?)",
                migrationKey
        );
    }

    private void clearAccountExperience(
            UserAccountRepository repository,
            String email,
            String seededExperience
    ) {
        repository.findByEmail(email)
                .filter(account -> seededExperience.equals(account.getExperience()))
                .ifPresent(account -> {
                    account.setExperience(null);
                    repository.save(account);
                });
    }

    private void clearProfileExperience(
            ProfileRepository repository,
            String userId,
            String seededExperience
    ) {
        repository.findByUserId(userId)
                .filter(profile -> seededExperience.equals(profile.getExperience()))
                .ifPresent(profile -> {
                    profile.setExperience(null);
                    repository.save(profile);
                });
    }

    private boolean isGeneratedSignupExperience(Map<String, Object> experience) {
        String title = String.valueOf(experience.getOrDefault("title", ""));
        String description = String.valueOf(experience.getOrDefault("description", ""));
        return title.endsWith(" Artist")
                && description.startsWith("Professional ")
                && description.endsWith(" experience in cinema and entertainment.");
    }
}
