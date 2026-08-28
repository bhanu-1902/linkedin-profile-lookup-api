package com.profilelookup.infrastructure.config;

import com.profilelookup.domain.ProfileSource;
import com.profilelookup.infrastructure.fixture.FixtureProfileSource;
import com.profilelookup.infrastructure.stub.StubProfileSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the application starts successfully for each
 * {@code profile.source} value.
 */
class ProfileSourceWiringTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ProfileSourceConfig.class);

    @Test
    void defaultsToFixtureSourceWhenPropertyIsUnset() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ProfileSource.class);
            assertThat(context.getBean(ProfileSource.class)).isInstanceOf(FixtureProfileSource.class);
        });
    }

    @Test
    void selectsStubSourceWhenConfigured() {
        contextRunner.withPropertyValues("profile.source=stub").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ProfileSource.class);
            assertThat(context.getBean(ProfileSource.class)).isInstanceOf(StubProfileSource.class);
        });
    }
}
