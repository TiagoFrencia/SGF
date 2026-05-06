package com.sgf.modules.auth.service;

import com.sgf.audit.service.AuditService;
import com.sgf.modules.auth.domain.UserAccountRepository;
import com.sgf.modules.auth.web.AuthResponse;
import com.sgf.modules.auth.web.LoginRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserAccountRepository userAccountRepository;
    private final JwtService jwtService;
    private final AuditService auditService;

    public AuthService(AuthenticationManager authenticationManager,
                       UserAccountRepository userAccountRepository,
                       JwtService jwtService,
                       AuditService auditService) {
        this.authenticationManager = authenticationManager;
        this.userAccountRepository = userAccountRepository;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        SgfUserPrincipal principal = userAccountRepository.findByUsernameAndActiveTrue(request.username())
                .map(SgfUserPrincipal::new)
                .orElseThrow();
        String token = jwtService.generateToken(principal);
        auditService.record(principal.getUsername(), "LOGIN", "USER", principal.getId(), "{\"username\":\"" + principal.getUsername() + "\"}");
        return new AuthResponse(token, principal.getUsername(), principal.getAuthorities().stream().map(Object::toString).toList());
    }
}

