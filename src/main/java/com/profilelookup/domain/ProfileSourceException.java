package com.profilelookup.domain;

import java.time.Duration;
import java.util.Optional;

/**
 * Thrown by a {@link ProfileSource} for a genuine failure -- distinct from
 * the normal, expected "no profile for this URL" case, which is
 * {@code Optional.empty()}, not an exception.
 *
 * Carries an optional {@link FailureType} classification and an optional
 * retry interval so a future live adapter can report *why* it failed
 * (unauthenticated, unavailable, rate-limited by the provider, or an
 * unexpected provider response) without leaking any provider-specific
 * detail -- see openspec/changes/prepare-live-profile-source/design.md,
 * "Keep ProfileSource as the sole extension point." An exception with no
 * classification (the original two-argument constructors) is treated as an
 * unclassified internal failure -- e.g. the fixture file being unreadable
 * or malformed -- and maps to a generic 500, not one of the four stable
 * source-failure responses.
 */
public class ProfileSourceException extends RuntimeException {

    /** Why a configured source could not resolve a request, at a level safe to expose to callers. */
    public enum FailureType {
        /** The source could not authenticate with its provider (e.g. missing/expired/invalid credentials). */
        UNAUTHENTICATED,
        /** The source is temporarily unreachable or otherwise unable to serve requests right now. */
        UNAVAILABLE,
        /** The provider itself is rate-limiting this source's requests. */
        RATE_LIMITED,
        /** The provider returned a response the source could not make sense of. */
        UPSTREAM_ERROR
    }

    private final FailureType failureType;
    private final Duration retryAfter;

    public ProfileSourceException(String message) {
        this(message, null, null, null);
    }

    public ProfileSourceException(String message, Throwable cause) {
        this(message, cause, null, null);
    }

    /** A classified failure with no known retry interval. */
    public ProfileSourceException(String message, FailureType failureType) {
        this(message, null, failureType, null);
    }

    /** A classified failure with a known retry interval (typically {@link FailureType#RATE_LIMITED}). */
    public ProfileSourceException(String message, FailureType failureType, Duration retryAfter) {
        this(message, null, failureType, retryAfter);
    }

    private ProfileSourceException(String message, Throwable cause, FailureType failureType, Duration retryAfter) {
        super(message, cause);
        this.failureType = failureType;
        this.retryAfter = retryAfter;
    }

    public Optional<FailureType> failureType() {
        return Optional.ofNullable(failureType);
    }

    public Optional<Duration> retryAfter() {
        return Optional.ofNullable(retryAfter);
    }
}
