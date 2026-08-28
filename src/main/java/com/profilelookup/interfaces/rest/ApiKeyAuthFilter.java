package com.profilelookup.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Plain servlet filter, not Spring Security -- see design.md, Decisions,
 * for why. A single header comparison doesn't need a security framework;
 * it needs a filter that either lets the request through or doesn't.
 *
 * Excludes {@code /v1/auth/linkedin/**}: those two endpoints are reached
 * by a browser following a redirect (the user's own browser hitting
 * /login, then LinkedIn's server redirecting that same browser to
 * /callback) -- neither caller can attach a custom X-API-Key header, so
 * gating them the same way as the JSON /v1/profile endpoint would make
 * the OIDC sign-in flow permanently 401. CSRF protection for that flow
 * comes from OAuthStateStore's single-use state parameter instead.
 */
@Component
@Order(1)
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String LINKEDIN_AUTH_PATH_PREFIX = "/v1/auth/linkedin/";

    private final Set<String> validApiKeys;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApiKeyAuthFilter(
            org.springframework.core.env.Environment env) {
        String configured = env.getProperty("profile.api-keys", "");
        this.validApiKeys = Arrays.stream(configured.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        if (!uri.startsWith("/v1/") || uri.startsWith(LINKEDIN_AUTH_PATH_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader(API_KEY_HEADER);
        if (apiKey == null || !validApiKeys.contains(apiKey)) {
            writeUnauthorized(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "Missing or invalid API key. Set the X-API-Key header.");
        problem.setType(URI.create("https://profilelookup.example/problems/unauthorized"));

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/problem+json");
        objectMapper.writeValue(response.getWriter(), problem);
    }
}
