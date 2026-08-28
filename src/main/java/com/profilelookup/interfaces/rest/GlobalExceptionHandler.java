package com.profilelookup.interfaces.rest;

import com.profilelookup.infrastructure.oidc.LinkedInProviderFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    @ExceptionHandler(LinkedInInvalidStateException.class)
    public ProblemDetail handleLinkedInInvalidState(LinkedInInvalidStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "The state parameter is missing, unrecognized, or already used. "
                        + "Start the sign-in flow again at /v1/auth/linkedin/login.");
        problem.setType(URI.create("https://profilelookup.example/problems/invalid-oauth-state"));
        return problem;
    }

    @ExceptionHandler(LinkedInConsentDeclinedException.class)
    public ProblemDetail handleLinkedInConsentDeclined(LinkedInConsentDeclinedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "LinkedIn sign-in was not completed: the user declined consent or the callback "
                        + "carried no authorization code.");
        problem.setType(URI.create("https://profilelookup.example/problems/consent-declined"));
        problem.setProperty("providerError", ex.getProviderError());
        return problem;
    }

    @ExceptionHandler(LinkedInProviderFailureException.class)
    public ProblemDetail handleLinkedInProviderFailure(LinkedInProviderFailureException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY,
                "LinkedIn did not return a usable response for the sign-in request. No profile was recorded.");
        problem.setType(URI.create("https://profilelookup.example/problems/provider-failure"));
        return problem;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFound(NoResourceFoundException ex) {
        // Only reachable at all for /v1/auth/linkedin/** -- every other
        // /v1/** path is intercepted by ApiKeyAuthFilter before dispatch
        // even attempts a handler lookup. Without this, an unmapped
        // route (e.g. requesting the OIDC endpoints while
        // profile.source isn't oidc) would fall into the generic
        // handleUnexpected 500 below instead of a correct 404 -- caught
        // by an actual local run, not by inspection.
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
