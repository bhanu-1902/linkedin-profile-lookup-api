package com.profilelookup.interfaces.rest.dto;

/**
 * Confirmation body for a completed LinkedIn OIDC callback -- not the
 * profile itself, just enough to tell the caller where to fetch it next
 * (the existing {@code /v1/profile} endpoint).
 */
public record LinkedInCallbackResponse(String status, String linkedInUrl, String message) {
}
