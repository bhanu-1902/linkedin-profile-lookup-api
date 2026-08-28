package com.profilelookup.infrastructure.oidc;

/**
 * The whole of what LinkedIn's OIDC userinfo endpoint gives us that this
 * adapter uses. {@code sub} is an opaque member ID, not a public profile
 * URL -- see design.md for why the caller's own asserted URL, not this
 * response, is what the consented profile ends up keyed by.
 */
public record LinkedInUserInfo(String sub, String name, String picture) {
}
