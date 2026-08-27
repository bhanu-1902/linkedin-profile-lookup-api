package com.profilelookup.infrastructure;

import com.profilelookup.domain.ProfileSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The contract every {@link ProfileSource} implementation must satisfy,
 * regardless of adapter. Both {@code FixtureProfileSourceTest} and
 * {@code StubProfileSourceTest} extend this and supply their own
 * instance via {@link #source()} -- if either adapter breaks the
 * contract, this test fails for that adapter specifically, which is the
 * whole point of testing against the port rather than each
 * implementation's own ad hoc assertions.
 *
 * This is what OpenSpec's "Honest handling of unavailable profiles"
 * requirement (specs/profile-lookup/spec.md) becomes as an executable
 * test.
 */
public abstract class ProfileSourceContractTest {

    protected abstract ProfileSource source();

    @Test
    void unknownUrlReturnsEmptyRatherThanThrowing() {
        var result = source().findByUrl("https://www.linkedin.com/in/definitely-not-a-known-fixture");

        assertThat(result).isEmpty();
    }
}
