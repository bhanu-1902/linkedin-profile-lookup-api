package com.profilelookup.infrastructure.oidc;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-issued, single-use CSRF state for the LinkedIn OAuth callback --
 * same in-memory-map pattern as {@code RateLimitFilter}'s buckets, and
 * the same documented single-instance limitation. A state is consumed
 * (removed) the first time it is checked, valid or not, so replaying a
 * used state always fails.
 *
 * Carries the caller's asserted {@code profileUrl} alongside the state
 * value, since that is the one piece of information the login request
 * has that the callback request doesn't -- see design.md, "The login
 * endpoint requires the caller's own profileUrl up front."
 */
public class OAuthStateStore {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);

    private final ConcurrentHashMap<String, IssuedState> statesByValue = new ConcurrentHashMap<>();
    private final Duration ttl;
    private final Clock clock;

    public OAuthStateStore() {
        this(DEFAULT_TTL, Clock.systemUTC());
    }

    OAuthStateStore(Duration ttl, Clock clock) {
        this.ttl = ttl;
        this.clock = clock;
    }

    public String issue(String profileUrl) {
        String state = UUID.randomUUID().toString();
        statesByValue.put(state, new IssuedState(clock.instant().plus(ttl), profileUrl));
        return state;
    }

    /**
     * Removes and returns the profile URL for a valid, unexpired state.
     * Empty for a missing, unrecognized, already-consumed, or expired
     * state -- all four cases are indistinguishable to the caller,
     * deliberately: none of them should attempt a token exchange.
     */
    public Optional<String> consume(String state) {
        if (state == null) {
            return Optional.empty();
        }
        IssuedState issued = statesByValue.remove(state);
        if (issued == null || clock.instant().isAfter(issued.expiresAt())) {
            return Optional.empty();
        }
        return Optional.of(issued.profileUrl());
    }

    private record IssuedState(Instant expiresAt, String profileUrl) {
    }
}
