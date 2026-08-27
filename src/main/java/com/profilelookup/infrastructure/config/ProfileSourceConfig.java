package com.profilelookup.infrastructure.config;

import com.profilelookup.domain.ProfileSource;
import com.profilelookup.infrastructure.fixture.FixtureProfileSource;
import com.profilelookup.infrastructure.stub.StubProfileSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * The Factory: selects which {@link ProfileSource} adapter Spring wires
 * into {@code ProfileLookupService}, based on the {@code profile.source}
 * property. This is the ONE place in the whole application that knows
 * both adapters exist. Everything upstream (the use case, the
 * controller) only ever sees the {@link ProfileSource} interface.
 *
 * profile.source=fixture (default) -> FixtureProfileSource
 * profile.source=stub              -> StubProfileSource
 */
@Configuration
public class ProfileSourceConfig {

    @Bean
    @ConditionalOnProperty(name = "profile.source", havingValue = "fixture", matchIfMissing = true)
    public ProfileSource fixtureProfileSource(
            @Value("${profile.fixture-path:classpath:fixtures/sample-profiles.json}") Resource fixtureResource) {
        return new FixtureProfileSource(fixtureResource);
    }

    @Bean
    @ConditionalOnProperty(name = "profile.source", havingValue = "stub")
    public ProfileSource stubProfileSource() {
        return new StubProfileSource();
    }
}
