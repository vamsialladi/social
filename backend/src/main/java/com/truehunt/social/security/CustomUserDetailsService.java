package com.truehunt.social.security;

import com.truehunt.social.model.UserAccountEntity;
import com.truehunt.social.repository.UserAccountRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    public CustomUserDetailsService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        String normalized = usernameOrEmail.trim().toLowerCase(Locale.ROOT);
        UserAccountEntity user = userAccountRepository.findByEmail(normalized)
                .or(() -> userAccountRepository.findByPublicUserId(usernameOrEmail))
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email or ID: " + usernameOrEmail));

        return UserPrincipal.create(user);
    }
}

