package com.profilelookup.interfaces.rest;

/**
 * Thrown when LinkedIn's callback carries an {@code error} parameter
 * (the user declined consent) instead of an authorization code, or omits
 * the code without an explicit error -- a business decision by the user,
 * not a provider failure, so it gets its own type rather than folding
 * into {@code LinkedInProviderFailureException}.
 */
public class LinkedInConsentDeclinedException extends RuntimeException {

    private final String providerError;

    public LinkedInConsentDeclinedException(String providerError) {
        super("LinkedIn sign-in was not completed: " + providerError);
        this.providerError = providerError;
    }

    public String getProviderError() {
        return providerError;
    }
}
