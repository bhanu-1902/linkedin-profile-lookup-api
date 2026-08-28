package com.profilelookup.domain;

import java.util.Optional;

/**
 * THE port. Every other design decision in this project exists to keep
 * this interface the only substitutable boundary.
 *
 * Contract: given a syntactically valid LinkedIn profile URL, either
 * return the profile this source can supply, or return empty -- never
 * throw for "I don't have this one," only for genuine failure (see
 * {@link ProfileSourceException}). Implementations must not perform any
 * action LinkedIn's terms of service prohibit; see
 * openspec/changes/archive/2026-08-28-add-profile-lookup/design.md,
 * "Rejected approach."
 */
public interface ProfileSource {

    Optional<Profile> findByUrl(String linkedInUrl);
}
