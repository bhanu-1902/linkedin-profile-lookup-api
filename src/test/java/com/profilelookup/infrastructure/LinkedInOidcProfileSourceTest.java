package com.profilelookup.infrastructure;

import com.profilelookup.domain.Profile;
import com.profilelookup.domain.ProfileSource;
import com.profilelookup.infrastructure.oidc.LinkedInOidcProfileSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LinkedInOidcProfileSourceTest extends ProfileSourceContractTest {

    private final LinkedInOidcProfileSource source = new LinkedInOidcProfileSource();

    @Override
    protected ProfileSource source() {
        return source;
    }

    @Test
    void returnsTheConsentedProfileForItsExactUrlOnly() {
        Profile consented = Profile.builder("https://www.linkedin.com/in/consented-user")
                .name("Consented User")
                .build();
        source.recordConsentedProfile(consented);

        assertThat(source.findByUrl("https://www.linkedin.com/in/consented-user")).contains(consented);
        assertThat(source.findByUrl("https://www.linkedin.com/in/someone-else")).isEmpty();
    }

    @Test
    void mostRecentlyConsentedProfileReplacesThePreviousOne() {
        source.recordConsentedProfile(
                Profile.builder("https://www.linkedin.com/in/first").name("First").build());
        source.recordConsentedProfile(
                Profile.builder("https://www.linkedin.com/in/second").name("Second").build());

        assertThat(source.findByUrl("https://www.linkedin.com/in/first")).isEmpty();
        assertThat(source.findByUrl("https://www.linkedin.com/in/second")).isPresent();
    }
}
