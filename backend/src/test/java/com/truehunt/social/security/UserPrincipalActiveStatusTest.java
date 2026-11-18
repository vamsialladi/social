package com.truehunt.social.security;

import com.truehunt.social.model.UserAccountEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserPrincipalActiveStatusTest {

    @Test
    void disabledMemberAccountIsNotEnabled() {
        UserAccountEntity account = new UserAccountEntity();
        account.setEmail("member@example.com");
        account.setRole("TALENT");
        account.setActive(false);

        assertFalse(UserPrincipal.create(account).isEnabled());
    }

    @Test
    void newMemberAccountIsEnabledByDefault() {
        UserAccountEntity account = new UserAccountEntity();

        assertTrue(UserPrincipal.create(account).isEnabled());
    }
}
