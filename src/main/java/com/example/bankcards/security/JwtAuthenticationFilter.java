package com.example.bankcards.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final int BEARER_PREFIX_LENGTH = BEARER_PREFIX.length();
    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final JwtProvider jwtProvider;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        log.debug("JWT filter processing request: {} {}", request.getMethod(), path);

        try {
            String token = extractTokenFromRequest(request);

            if (token != null) {
                authenticateUser(request, token, path);
            } else {
                log.debug("No valid Bearer token found for path: {}", path);
            }
        } catch (Exception e) {
            log.error("JWT authentication error for path {}: {}", path, e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX_LENGTH);
        }

        return null;
    }

    private void authenticateUser(HttpServletRequest request, String token, String path) {
        if (!jwtProvider.validateToken(token)) {
            log.warn("Invalid JWT token for path: {}", path);
            return;
        }

        String username = jwtProvider.extractUsername(token);

        if (!StringUtils.hasText(username)) {
            log.warn("Empty username extracted from JWT token for path: {}", path);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            log.debug("User already authenticated, skipping JWT authentication for path: {}", path);
            return;
        }

        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            setAuthentication(request, userDetails);
            log.debug("JWT authentication successful for user: {}", username);
        } catch (UsernameNotFoundException e) {
            log.warn("User not found during JWT authentication: {} for path: {}", username, path);
        }
    }

    private void setAuthentication(HttpServletRequest request, UserDetails userDetails) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}
