package com.sgf.modules.auth.web;

import java.util.List;

public record AuthResponse(
        String accessToken,
        String username,
        List<String> roles
) {
}

