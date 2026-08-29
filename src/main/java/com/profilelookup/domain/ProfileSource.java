package com.profilelookup.domain;

import java.util.Optional;

/**
 * The port between the application layer and any source of profile data.
 *
 * Contract: given a syntactically valid LinkedIn profile URL, either
 * return the profile this source can supply, or return empty -- never
 * throw for "I don't have this one," only for genuine failure (see
 * {@link ProfileSourceException}).
 */
public interface ProfileSource {

    Optional<Profile> findByUrl(String linkedInUrl);
}
