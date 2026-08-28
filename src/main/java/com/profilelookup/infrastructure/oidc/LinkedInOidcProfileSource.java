package com.profilelookup.infrastructure.oidc;

import com.profilelookup.domain.LinkedInProfileUrls;
import com.profilelookup.domain.Profile;
import com.profilelookup.domain.ProfileSource;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Adapter: serves exactly one profile -- whichever one most recently
 * completed the LinkedIn OIDC consent flow on this instance (see
 * {@code interfaces.rest.LinkedInAuthController}) -- and
 * {@code Optional.empty()} for every other URL, identically to
 * {@code StubProfileSource}. Single mutable slot, not a session-keyed
 * map; see design.md for why that's the right call here rather than an
 * unfinished multi-user feature.
 */
public class LinkedInOidcProfileSource implements ProfileSource {

    private final AtomicReference<Profile> consentedProfile = new AtomicReference<>();

    public void recordConsentedProfile(Profile profile) {
        consentedProfile.set(profile);
    }

    @Override
    public Optional<Profile> findByUrl(String linkedInUrl) {
        return LinkedInProfileUrls.canonicalize(linkedInUrl)
                .flatMap(canonical -> {
                    Profile current = consentedProfile.get();
                    return current != null && current.linkedInUrl().equals(canonical)
                            ? Optional.of(current)
                            : Optional.empty();
                });
    }
}
