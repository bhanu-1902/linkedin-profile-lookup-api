package com.profilelookup.interfaces.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

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
