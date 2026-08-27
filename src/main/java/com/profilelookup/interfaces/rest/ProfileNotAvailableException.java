package com.profilelookup.interfaces.rest;

/**
 * Thrown by the controller, not the domain: "no live data source for
 * this URL" is an HTTP-response concern (what status/body to send),
 * not a domain concept. The domain layer's signal for the same
 * situation is simply {@code Optional.empty()} -- see
 * {@code ProfileSource.findByUrl}.
 */
public class ProfileNotAvailableException extends RuntimeException {

    private final String requestedUrl;

    public ProfileNotAvailableException(String requestedUrl) {
        super("No live data source configured for: " + requestedUrl);
        this.requestedUrl = requestedUrl;
    }

    public String getRequestedUrl() {
        return requestedUrl;
    }
}
