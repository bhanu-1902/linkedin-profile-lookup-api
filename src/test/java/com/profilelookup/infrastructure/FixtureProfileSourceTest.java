package com.profilelookup.infrastructure;

import com.profilelookup.domain.Profile;
import com.profilelookup.domain.ProfileSource;
import com.profilelookup.infrastructure.fixture.FixtureProfileSource;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FixtureProfileSourceTest extends ProfileSourceContractTest {

    private final FixtureProfileSource fixtureSource =
            new FixtureProfileSource(new ClassPathResource("fixtures/sample-profiles.json"));

    @Override
    protected ProfileSource source() {
        return fixtureSource;
    }

    @Test
    void returnsTheBundledSampleProfileByExactUrl() {
        Optional<Profile> result =
                fixtureSource.findByUrl("https://www.linkedin.com/in/example-profile");

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("Alex Example");
        assertThat(result.get().skills()).contains("Java", "Spring Boot");
    }

    @Test
    void ignoresQueryStringAndTrailingSlashWhenMatching() {
        assertThat(fixtureSource.findByUrl(
                "https://www.linkedin.com/in/example-profile/?trk=nav"))
                .isPresent();
    }
}
