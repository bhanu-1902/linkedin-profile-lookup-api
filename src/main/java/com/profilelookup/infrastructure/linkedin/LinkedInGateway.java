package com.profilelookup.infrastructure.linkedin;
public interface LinkedInGateway {
    UpstreamResponse fetch(String canonicalProfileUrl, String handle);
}
