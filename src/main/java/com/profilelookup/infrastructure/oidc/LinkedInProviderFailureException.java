package com.profilelookup.infrastructure.oidc;

/**
 * Thrown when LinkedIn's token-exchange or userinfo call fails (network
 * error, non-2xx response, or a response missing the fields the flow
 * needs) -- distinct from a declined-consent callback, which is the
 * user's choice, not a provider failure. See
 * {@code interfaces.rest.GlobalExceptionHandler}, which maps this to a
 * 502 problem+json response without leaking upstream detail.
 */
public class LinkedInProviderFailureException extends RuntimeException {

    public LinkedInProviderFailureException(String message) {
        super(message);
    }

    public LinkedInProviderFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
