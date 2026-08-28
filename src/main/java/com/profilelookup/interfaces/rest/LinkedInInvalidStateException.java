package com.profilelookup.interfaces.rest;

/**
 * Thrown by {@code LinkedInAuthController} when the callback's
 * {@code state} parameter is missing, unrecognized, or already consumed
 * -- the CSRF check. Deliberately carries no detail about which of those
 * three it was; that distinction has no legitimate use for a caller and
 * a real one for an attacker probing the flow.
 */
public class LinkedInInvalidStateException extends RuntimeException {

    public LinkedInInvalidStateException() {
        super("Missing, unrecognized, or already-consumed OAuth state parameter.");
    }
}
