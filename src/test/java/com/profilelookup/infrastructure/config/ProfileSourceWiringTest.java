package com.profilelookup.infrastructure.config;

import com.profilelookup.domain.ProfileSource;
import com.profilelookup.infrastructure.fixture.FixtureProfileSource;
import com.profilelookup.infrastructure.oidc.LinkedInOidcProfileSource;
import com.profilelookup.infrastructure.stub.StubProfileSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the application starts successfully for each of the three
 * {@code profile.source} values, and fails fast -- rather than with a
 * later NullPointerException -- when {@code oidc} is selected without
 * its required LinkedIn credentials. See
 * openspec/changes/add-oidc-self-lookup/tasks.md, 5.1 and 5.2.
 */
class ProfileSourceWiringTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ProfileSourceConfig.class, LinkedInOidcConfig.class);

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

    @Test
    void selectsOidcSourceWhenConfiguredWithCredentials() {
        contextRunner.withPropertyValues(
                "profile.source=oidc",
                "linkedin.client-id=test-client",
                "linkedin.client-secret=test-secret",
                "linkedin.redirect-uri=http://localhost:8080/v1/auth/linkedin/callback"
        ).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ProfileSource.class);
            assertThat(context.getBean(ProfileSource.class)).isInstanceOf(LinkedInOidcProfileSource.class);
        });
    }

    @Test
    void oidcSourceFailsFastWhenCredentialsAreMissing() {
        contextRunner.withPropertyValues("profile.source=oidc")
                .run(context -> assertThat(context).hasFailed());
    }
}
