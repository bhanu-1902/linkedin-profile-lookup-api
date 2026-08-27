package com.profilelookup.application;

import com.profilelookup.domain.Profile;
import com.profilelookup.domain.ProfileSource;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the use case depends only on the ProfileSource interface: this
 * test never imports FixtureProfileSource or StubProfileSource, only a
 * hand-written fake implementing the port.
 */
class ProfileLookupServiceTest {

    @Test
    void delegatesLookupToWhicheverProfileSourceIsInjected() {
        Profile expected = Profile.builder("https://www.linkedin.com/in/someone").name("Someone").build();
        ProfileSource fake = url -> Optional.of(expected);
        ProfileLookupService service = new ProfileLookupService(fake);

        Optional<Profile> result = service.lookup("https://www.linkedin.com/in/someone");

        assertThat(result).contains(expected);
    }

    @Test
    void returnsEmptyWhenSourceHasNothingForTheUrl() {
        ProfileSource fake = url -> Optional.empty();
        ProfileLookupService service = new ProfileLookupService(fake);

        assertThat(service.lookup("https://www.linkedin.com/in/nobody")).isEmpty();
    }
}
