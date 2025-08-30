package com.example.bankcards.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                      AccessDeniedException accessDeniedException) throws IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (isUnauthenticated(authentication)) {
            handleUnauthenticated(request, response);
        } else {
            handleAccessDenied(request, response, authentication);
        }
    }

    private boolean isUnauthenticated(Authentication authentication) {
        return authentication == null ||
               !authentication.isAuthenticated() ||
               "anonymousUser".equals(authentication.getPrincipal());
    }

    private void handleUnauthenticated(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        log.warn("Unauthorized access attempt: {} {} - Authentication required",
                request.getMethod(), request.getRequestURI());

        Map<String, Object> errorResponse = Map.of(
                "timestamp", getCurrentTimestamp(),
                "status", HttpStatus.UNAUTHORIZED.value(),
                "error", "Unauthorized",
                "message", "Authentication is required to access this resource"
        );

        writeErrorResponse(response, HttpStatus.UNAUTHORIZED, errorResponse);
    }

    private void handleAccessDenied(HttpServletRequest request, HttpServletResponse response,
                                   Authentication authentication) throws IOException {

        log.warn("Access denied for user '{}': {} {} - Insufficient privileges",
                authentication.getName(), request.getMethod(), request.getRequestURI());

        Map<String, Object> errorResponse = Map.of(
                "timestamp", getCurrentTimestamp(),
                "status", HttpStatus.FORBIDDEN.value(),
                "error", "Insufficient Privileges",
                "message", "You don't have sufficient privileges to access this resource"
        );

        writeErrorResponse(response, HttpStatus.FORBIDDEN, errorResponse);
    }

    private void writeErrorResponse(HttpServletResponse response, HttpStatus status,
                                   Map<String, Object> errorResponse) throws IOException {

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String json = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(json);
    }

    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
