package com.profilelookup.infrastructure;

import com.profilelookup.domain.Profile;
import com.profilelookup.domain.ProfileSource;
import com.profilelookup.domain.ProfileSourceException;

import java.time.Duration;
import java.util.Optional;

/**
 * Test-only {@link ProfileSource}: throws whichever classified
 * {@link ProfileSourceException} the test last configured, so
 * {@code SourceFailureProblemResponseTest} can drive a real HTTP request
 * through the real controller and exception handler for each of the four
 * classifications, rather than asserting against the handler in isolation.
 * Never shipped -- lives under {@code src/test}.
 */
public class FaultyProfileSource implements ProfileSource {

    private volatile ProfileSourceException.FailureType nextFailureType;
    private volatile Duration nextRetryAfter;

    public void failNextWith(ProfileSourceException.FailureType failureType) {
        failNextWith(failureType, null);
    }

    public void failNextWith(ProfileSourceException.FailureType failureType, Duration retryAfter) {
        this.nextFailureType = failureType;
        this.nextRetryAfter = retryAfter;
    }

    @Override
    public Optional<Profile> findByUrl(String linkedInUrl) {
        throw nextRetryAfter == null
                ? new ProfileSourceException("simulated source failure", nextFailureType)
                : new ProfileSourceException("simulated source failure", nextFailureType, nextRetryAfter);
    }
}
