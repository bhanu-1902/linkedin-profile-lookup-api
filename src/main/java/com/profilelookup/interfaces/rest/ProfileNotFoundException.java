package com.profilelookup.interfaces.rest;
public class ProfileNotFoundException extends RuntimeException {
    private final String requestedUrl;
    public ProfileNotFoundException(String requestedUrl) { super("No profile available for: " + requestedUrl); this.requestedUrl = requestedUrl; }
    public String getRequestedUrl() { return requestedUrl; }
}
