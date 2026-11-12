package com.truehunt.social.service;

import com.truehunt.social.exception.BadRequestException;
import com.truehunt.social.model.OpportunityApplicationEntity;
import com.truehunt.social.model.PostEntity;
import com.truehunt.social.model.ProfileEntity;
import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.repository.OpportunityApplicationRepository;
import com.truehunt.social.repository.PostRepository;
import com.truehunt.social.repository.ProfileRepository;
import com.truehunt.social.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(AdminService.class)
class AdminServiceDeletionTest {

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private OpportunityApplicationRepository applicationRepository;

    @Test
    void deletesAccountProfilePostsAndApplicationRowsFromDatabase() {
        UserAccountEntity account = new UserAccountEntity();
        account.setEmail("member@example.com");
        account.setPasswordHash("hash");
        account.setName("Member");
        account.setPublicUserId("member-1");
        account.setRole("USER");
        account = userAccountRepository.saveAndFlush(account);

        ProfileEntity profile = new ProfileEntity();
        profile.setUserId("member-1");
        profile.setEmail("member@example.com");
        profile.setName("Member");
        profileRepository.saveAndFlush(profile);

        PostEntity post = new PostEntity();
        post.setAuthorId("member-1");
        post.setType("opportunity");
        postRepository.saveAndFlush(post);
        applicationRepository.saveAndFlush(new OpportunityApplicationEntity(post.getId(), "member@example.com"));

        adminService.deleteAccount(account.getId(), "admin@example.com");

        assertFalse(userAccountRepository.findById(account.getId()).isPresent());
        assertFalse(profileRepository.findByUserId("member-1").isPresent());
        assertEquals(0, postRepository.count());
        assertEquals(0, applicationRepository.count());
    }

    @Test
    void preventsAdminFromDeletingTheirOwnAccount() {
        UserAccountEntity admin = new UserAccountEntity();
        admin.setEmail("admin@example.com");
        admin.setPasswordHash("hash");
        admin.setName("Admin");
        admin.setRole("ADMIN");
        admin = userAccountRepository.saveAndFlush(admin);

        Long adminId = admin.getId();
        assertThrows(BadRequestException.class, () -> adminService.deleteAccount(adminId, "ADMIN@example.com"));
        assertEquals(1, userAccountRepository.count());
    }

    @Test
    void doesNotListAdministratorAsSocialMember() {
        UserAccountEntity admin = new UserAccountEntity();
        admin.setEmail("admin@example.com");
        admin.setPasswordHash("hash");
        admin.setName("Admin");
        admin.setRole("ADMIN");
        userAccountRepository.save(admin);

        UserAccountEntity member = new UserAccountEntity();
        member.setEmail("member@example.com");
        member.setPasswordHash("hash");
        member.setName("Member");
        member.setRole("USER");
        userAccountRepository.saveAndFlush(member);

        assertEquals(1, adminService.getAccounts().size());
        assertTrue(adminService.getAccounts().stream()
                .anyMatch(account -> "member@example.com".equals(account.get("email"))));
    }
}
