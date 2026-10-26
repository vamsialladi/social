package com.truehunt.social.config;

import com.truehunt.social.model.*;
import com.truehunt.social.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
            NotificationRepository notificationRepository
    ) {
        return args -> {
            // 1. Seed Profile
            if (profileRepository.findByUserId("me").isEmpty()) {
                ProfileEntity profile = new ProfileEntity();
                profile.setUserId("me");
                profile.setName("Chandrabose Alladi");
                profile.setPhone("+91 9876543210");
                profile.setEmail("chandrabose@truehunt.com");
                profile.setExperience("5");
                profile.setAddress("Hyderabad, Telangana, India");
                profile.setLanguages(List.of("Telugu", "English", "Hindi"));
                profile.setOtherLanguage("");
                profile.setTalents(List.of("Acting", "Direction", "Screenwriting"));
                profile.setWorkTypes(List.of("Cinema", "Web series", "Short films"));
                profile.setProfileImageUrl("");
                profile.setCoverImageUrl("");
                profile.setVideoUrls(List.of("https://www.youtube.com/watch?v=sample1"));

                Map<String, Object> exp = new HashMap<>();
                exp.put("id", "exp-1");
                exp.put("projectName", "Cinema Scope Production");
                exp.put("organizationName", "Northstar Films");
                exp.put("location", "Hyderabad");
                exp.put("workedAs", "Assistant Director & Writer");
                exp.put("startDate", "2023-01-01");
                exp.put("endDate", "2024-06-30");
                exp.put("highlights", "Supervised on-set screenwriting and artist auditions for upcoming web feature.");
                profile.setExperiences(List.of(exp));

                profileRepository.save(profile);
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

                ConversationEntity c3 = new ConversationEntity();
                c3.setName("Rohan Mehta");
                c3.setInitials("RM");
                c3.setColor("green");
                c3.setPreview("Are you available for a quick chat this week?");
                c3.setDate("Oct 08");
                c3.setCategory("InMail");
                c3.setUnread(false);
                c3.setSponsored(false);
                c3.setSubject("A quick chat this week?");
                c3.setDescription("Are you available for a quick chat this week?");
                c3.setMailbox("Focused");
                c3.setThread(List.of(
                        Map.of("id", "rohan-1", "content", "Are you available for a quick chat this week?", "direction", "incoming")
                ));
                conversationRepository.save(c3);

                ConversationEntity c4 = new ConversationEntity();
                c4.setName("Meera Kapoor");
                c4.setInitials("MK");
                c4.setColor("purple");
                c4.setPreview("I loved your recent work. Would love to collaborate.");
                c4.setDate("Oct 03");
                c4.setCategory("Connections");
                c4.setUnread(false);
                c4.setSponsored(false);
                c4.setSubject("Let’s collaborate");
                c4.setDescription("I loved your recent work. Would love to collaborate.");
                c4.setMailbox("Focused");
                c4.setThread(List.of(
                        Map.of("id", "meera-1", "content", "I loved your recent work. Would love to collaborate.", "direction", "incoming")
                ));
                conversationRepository.save(c4);
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
