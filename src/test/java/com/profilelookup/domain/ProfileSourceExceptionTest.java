package com.profilelookup.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A {@link ProfileSource} signals "no profile for this URL" with
 * {@code Optional.empty()} and a genuine failure by throwing -- these
 * tests verify the exception side carries enough information to
 * distinguish an unclassified internal error from a classified, safe-to
 * expose, potentially retryable source failure. See
 * {@code interfaces.rest.GlobalExceptionHandler} for where each
 * classification becomes an RFC 9457 response.
 */
class ProfileSourceExceptionTest {

    @Test
    void unclassifiedExceptionCarriesNoFailureTypeOrRetryInterval() {
        var ex = new ProfileSourceException("fixture file unreadable");

        assertThat(ex.failureType()).isEmpty();
        assertThat(ex.retryAfter()).isEmpty();
    }

    @Test
    void unclassifiedExceptionWithCauseAlsoCarriesNoFailureType() {
        var ex = new ProfileSourceException("fixture file unreadable", new java.io.IOException("boom"));

        assertThat(ex.failureType()).isEmpty();
        assertThat(ex.getCause()).isInstanceOf(java.io.IOException.class);
    }

    @Test
    void classifiedExceptionWithoutRetryIntervalIsDistinctFromUnclassified() {
        var ex = new ProfileSourceException(
                "source could not authenticate", ProfileSourceException.FailureType.UNAUTHENTICATED);

        assertThat(ex.failureType()).contains(ProfileSourceException.FailureType.UNAUTHENTICATED);
        assertThat(ex.retryAfter()).isEmpty();
    }

    @Test
    void rateLimitedFailureCarriesARetryInterval() {
        var ex = new ProfileSourceException(
                "provider is rate-limiting this source",
                ProfileSourceException.FailureType.RATE_LIMITED,
                Duration.ofSeconds(30));

        assertThat(ex.failureType()).contains(ProfileSourceException.FailureType.RATE_LIMITED);
        assertThat(ex.retryAfter()).contains(Duration.ofSeconds(30));
    }

    @Test
    void everyClassificationIsDistinguishableFromTheOthers() {
        for (ProfileSourceException.FailureType type : ProfileSourceException.FailureType.values()) {
            var ex = new ProfileSourceException("simulated", type);
            assertThat(ex.failureType()).contains(type);
        }
    }

    // The other half of the distinction this class exists for: a source
    // reporting "I have nothing for this URL" never throws at all.
    @Test
    void absentProfileDataIsSignaledByEmptyOptionalNotAnException() {
        ProfileSource alwaysEmpty = url -> Optional.empty();

        assertThat(alwaysEmpty.findByUrl("https://www.linkedin.com/in/anyone")).isEmpty();
    }
}
