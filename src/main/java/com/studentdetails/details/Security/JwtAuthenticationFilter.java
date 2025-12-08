package com.studentdetails.details.Security;

import com.studentdetails.details.Domain.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT authentication filter that processes JWT tokens from requests.
 * Production-ready implementation that only accepts valid JWT tokens.
 * Supports both "Authorization: Bearer <token>" and "X-Auth-Token: <token>" headers.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("unused") // Suppress unused warning - class is used by Spring Framework
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = getTokenFromRequest(request);

            if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {
                String email = tokenProvider.getEmailFromToken(token);
                UserRole role = tokenProvider.getRoleFromToken(token);

                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    email,
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
                            );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Authenticated user: {} with role: {}", email, role);
                }
            } else if (StringUtils.hasText(token)) {
                log.warn("Invalid or expired JWT token provided");
            }
        } catch (Exception ex) {
            log.debug("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts JWT token from request.
     * Supports both "Authorization: Bearer <token>" (standard) and "X-Auth-Token: <token>" headers.
     *
     * @param request the HTTP request
     * @return the JWT token or null
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        // Try Authorization header first (RFC 6750 standard)
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // Fallback to X-Auth-Token header (for client/organization flexibility)
        // Note: This is less standard but provides flexibility for different client implementations
        String authToken = request.getHeader("X-Auth-Token");
        if (StringUtils.hasText(authToken)) {
            return authToken;
        }

        return null;
    }
}


