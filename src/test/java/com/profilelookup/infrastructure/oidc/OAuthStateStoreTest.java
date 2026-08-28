package com.profilelookup.infrastructure.oidc;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthStateStoreTest {

    private final OAuthStateStore store = new OAuthStateStore();

    @Test
    void issuedStateCanBeConsumedExactlyOnce() {
        String state = store.issue("https://www.linkedin.com/in/example");

        assertThat(store.consume(state)).contains("https://www.linkedin.com/in/example");
        assertThat(store.consume(state)).isEmpty();
    }

    @Test
    void unissuedStateIsRejected() {
        assertThat(store.consume("never-issued")).isEmpty();
    }

    @Test
    void nullStateIsRejected() {
        assertThat(store.consume(null)).isEmpty();
    }

    @Test
    void expiredStateIsRejectedEvenIfOtherwiseWellFormed() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        OAuthStateStore shortLived = new OAuthStateStore(Duration.ofMinutes(5), clock);
        String state = shortLived.issue("https://www.linkedin.com/in/example");

        clock.advance(Duration.ofMinutes(6));

        assertThat(shortLived.consume(state)).isEmpty();
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
