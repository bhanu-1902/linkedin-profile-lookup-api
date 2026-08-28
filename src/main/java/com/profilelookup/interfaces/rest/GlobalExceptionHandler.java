package com.profilelookup.interfaces.rest;

import com.profilelookup.domain.ProfileSourceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

/**
 * One handler for every /v1/** error case, all returning RFC 9457
 * problem+json via Spring's native ProblemDetail (Spring Framework 6+ --
 * no extra dependency). Never returns a stack trace or internal detail;
 * every {@code type} URI is a stable, documented identifier a client
 * can match on.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidProfileUrlException.class)
    public ProblemDetail handleInvalidUrl(InvalidProfileUrlException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "url must be a valid LinkedIn profile URL, e.g. https://www.linkedin.com/in/<handle>");
        problem.setType(URI.create("https://profilelookup.example/problems/validation-error"));
        problem.setProperty("submittedValue", ex.getSubmittedValue());
        return problem;
    }

    @ExceptionHandler(ProfileNotAvailableException.class)
    public ProblemDetail handleProfileNotAvailable(ProfileNotAvailableException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_IMPLEMENTED,
                "No live data source is configured for this profile. "
                        + "Only fixture-backed profiles are served in this deployment "
                        + "(see README, 'Known limitations').");
        problem.setType(URI.create("https://profilelookup.example/problems/no-live-data-source"));
        problem.setProperty("requestedUrl", ex.getRequestedUrl());
        return problem;
    }

    /**
     * A classified {@link ProfileSourceException} (see its javadoc) becomes
     * one of four stable, documented responses -- never the provider detail
     * that caused it. An unclassified exception (the fixture-load-failure
     * case) falls through to the same generic 500 as
     * {@link #handleUnexpected}, since {@code ProfileSourceException} would
     * otherwise shadow that handler for every instance, classified or not.
     */
    @ExceptionHandler(ProfileSourceException.class)
    public ResponseEntity<ProblemDetail> handleProfileSourceFailure(ProfileSourceException ex) {
        if (ex.failureType().isEmpty()) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
            problem.setType(URI.create("https://profilelookup.example/problems/internal-error"));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
        }

        return switch (ex.failureType().get()) {
            case UNAUTHENTICATED -> sourceFailureResponse(HttpStatus.SERVICE_UNAVAILABLE, "source-unauthenticated",
                    "The configured profile source could not authenticate with its provider.", ex.retryAfter());
            case UNAVAILABLE -> sourceFailureResponse(HttpStatus.SERVICE_UNAVAILABLE, "source-unavailable",
                    "The configured profile source is temporarily unavailable.", ex.retryAfter());
            case RATE_LIMITED -> sourceFailureResponse(HttpStatus.TOO_MANY_REQUESTS, "source-rate-limited",
                    "The configured profile source is being rate-limited by its provider.", ex.retryAfter());
            case UPSTREAM_ERROR -> sourceFailureResponse(HttpStatus.BAD_GATEWAY, "source-upstream-error",
                    "The configured profile source returned a response it could not use.", ex.retryAfter());
        };
    }

    private ResponseEntity<ProblemDetail> sourceFailureResponse(
            HttpStatus status, String typeSlug, String detail, Optional<Duration> retryAfter) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("https://profilelookup.example/problems/" + typeSlug));

        ResponseEntity.BodyBuilder response = ResponseEntity.status(status);
        retryAfter.ifPresent(interval -> response.header("Retry-After", String.valueOf(Math.max(1, interval.toSeconds()))));
        return response.body(problem);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFound(NoResourceFoundException ex) {
        // Reached for any genuinely unmapped route. Without this, that
        // case fell into the generic handleUnexpected 500 below instead of
        // a correct 404 -- caught by an actual local run, not by
        // inspection.
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, "No such endpoint.");
        problem.setType(URI.create("https://profilelookup.example/problems/not-found"));
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        // Deliberately generic: never leak stack traces or internal
        // detail in a response body.
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        problem.setType(URI.create("https://profilelookup.example/problems/internal-error"));
        return problem;
    }
}
