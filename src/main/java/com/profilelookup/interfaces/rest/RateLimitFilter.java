package com.profilelookup.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Token-bucket rate limiting, one bucket per API key (falls back to
 * remote address if no key was presented -- the auth filter, ordered
 * before this one, already rejects that case for /v1/**, so this only
 * matters for any future unauthenticated route).
 *
 * Uses bucket4j-core directly rather than a Spring-integration starter
 * -- see design.md, Decisions.
 *
 * Known limitation, stated plainly: buckets live in a
 * ConcurrentHashMap, so limits are per-instance. Fine for a demo;
 * documented in the README as the first thing to change (Bucket4j's
 * distributed mode + Redis) before running more than one instance.
 */
@Component
@Order(2)
public class RateLimitFilter extends OncePerRequestFilter {

    private final ConcurrentHashMap<String, Bucket> bucketsByKey = new ConcurrentHashMap<>();
    private final int capacity;
    private final Duration refillPeriod;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RateLimitFilter(
            @Value("${ratelimit.capacity:20}") int capacity,
            @Value("${ratelimit.refill-period-seconds:60}") long refillPeriodSeconds) {
        this.capacity = capacity;
        this.refillPeriod = Duration.ofSeconds(refillPeriodSeconds);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!request.getRequestURI().startsWith("/v1/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String bucketKey = bucketKeyFor(request);
        Bucket bucket = bucketsByKey.computeIfAbsent(bucketKey, k -> newBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            writeTooManyRequests(response, probe);
        }
    }

    private String bucketKeyFor(HttpServletRequest request) {
        String apiKey = request.getHeader("X-API-Key");
        return apiKey != null ? apiKey : request.getRemoteAddr();
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.intervally(capacity, refillPeriod));
        return Bucket.builder().addLimit(limit).build();
    }

    private void writeTooManyRequests(HttpServletResponse response, ConsumptionProbe probe) throws IOException {
        long retryAfterSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1;

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded. Retry after the indicated interval.");
        problem.setType(URI.create("https://profilelookup.example/problems/rate-limited"));

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType("application/problem+json");
        objectMapper.writeValue(response.getWriter(), problem);
    }
}
