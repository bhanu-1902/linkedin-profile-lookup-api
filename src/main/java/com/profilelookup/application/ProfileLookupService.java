package com.profilelookup.application;

import com.profilelookup.domain.Profile;
import com.profilelookup.domain.ProfileSource;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * The use case. Depends only on the {@link ProfileSource} port -- this
 * class has never seen {@code FixtureProfileSource} or
 * {@code StubProfileSource} by name, and never will. Spring injects
 * whichever adapter {@code profile.source} selects (see
 * {@code infrastructure.config.ProfileSourceConfig}).
 *
 * SRP: this class's only reason to change is "how a lookup is
 * orchestrated." It does not know how a profile is stored, sourced, or
 * about to be serialized.
 */
@Service
public class ProfileLookupService {

    private final ProfileSource profileSource;

    public ProfileLookupService(ProfileSource profileSource) {
        this.profileSource = profileSource;
    }

    public Optional<Profile> lookup(String linkedInUrl) {
        return profileSource.findByUrl(linkedInUrl);
    }
}
