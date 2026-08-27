package com.profilelookup.infrastructure.stub;

import com.profilelookup.domain.Profile;
import com.profilelookup.domain.ProfileSource;

import java.util.Optional;

/**
 * Adapter: always returns empty. This is the documented, honest stand-in
 * for "no live data source configured" -- see
 * openspec/changes/add-profile-lookup/design.md, "Rejected approach," for
 * why this is a deliberate architectural boundary rather than an
 * unfinished feature. The controller/exception-handler layer turns
 * "empty" into a typed 501 problem+json response, not a silent 404.
 */
public class StubProfileSource implements ProfileSource {

    @Override
    public Optional<Profile> findByUrl(String linkedInUrl) {
        return Optional.empty();
    }
}
