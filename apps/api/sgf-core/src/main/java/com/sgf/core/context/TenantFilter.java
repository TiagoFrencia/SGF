package com.sgf.core.context;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet filter that extracts the tenant ID from the JWT and sets it
 * in the TenantContext ThreadLocal for the duration of the request.
 *
 * Priority: X-Tenant-ID header (for internal services) > JWT claim > default
 */
@Component
public class TenantFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantFilter.class);
    private static final String DEFAULT_TENANT = "00000000-0000-0000-0000-000000000001";

    @Value("${sgf.tenant.header:X-Tenant-ID}")
    private String tenantHeader;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. Check explicit header (inter-service calls)
            String tenantId = request.getHeader(tenantHeader);

            // 2. Extract from JWT if not in header
            if (tenantId == null || tenantId.isBlank()) {
                tenantId = extractTenantFromJwt(request);
            }

            // 3. Fallback to default for dev/test
            if (tenantId == null || tenantId.isBlank()) {
                tenantId = DEFAULT_TENANT;
            }

            TenantContext.setTenantId(tenantId);
            log.trace("Tenant context set: {}", tenantId);

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String extractTenantFromJwt(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;

        try {
            String token = authHeader.substring(7);
            Claims claims = Jwts.parser()
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.get("tenant_id", String.class);
        } catch (Exception e) {
            log.debug("Could not extract tenant_id from JWT: {}", e.getMessage());
            return null;
        }
    }
}
