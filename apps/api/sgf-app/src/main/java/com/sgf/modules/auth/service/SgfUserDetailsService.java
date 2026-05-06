package com.sgf.modules.auth.service;

import com.sgf.modules.auth.domain.UserAccountRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class SgfUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    public SgfUserDetailsService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userAccountRepository.findByUsernameAndActiveTrue(username)
                .map(SgfUserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }
}

