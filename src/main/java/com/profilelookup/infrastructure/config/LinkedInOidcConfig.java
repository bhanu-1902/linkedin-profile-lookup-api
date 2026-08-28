package com.profilelookup.infrastructure.config;

import com.profilelookup.infrastructure.oidc.LinkedInOidcClient;
import com.profilelookup.infrastructure.oidc.OAuthStateStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Supporting beans for the LinkedIn OIDC adapter -- the state store and
 * the OIDC HTTP client -- wired only when {@code profile.source=oidc},
 * same condition {@link ProfileSourceConfig} uses to select the adapter
 * itself. This is also where the "fail fast, not a NullPointerException
 * three calls later" requirement is met: {@link LinkedInOidcClient}'s
 * constructor rejects blank credentials immediately, and that
 * constructor only ever runs when oidc mode is actually selected, so
 * running fixture/stub mode never requires LinkedIn credentials.
 *
 * Builds its own {@link RestClient} via the plain static factory rather
 * than injecting Spring Boot's auto-configured {@code RestClient.Builder}
 * bean -- that bean now lives in the separate {@code spring-boot-restclient}
 * module (Spring Boot 4 split it out of spring-boot-web), which this
 * project has no other reason to depend on. Same reasoning as {@code
 * FixtureProfileSource} building its own {@code ObjectMapper}: one fewer
 * dependency on which auto-configuration happens to be present.
 */
@Configuration
public class LinkedInOidcConfig {

    @Bean
    @ConditionalOnProperty(name = "profile.source", havingValue = "oidc")
    public OAuthStateStore oAuthStateStore() {
        return new OAuthStateStore();
    }

    @Bean
    @ConditionalOnProperty(name = "profile.source", havingValue = "oidc")
    public LinkedInOidcClient linkedInOidcClient(
            @Value("${linkedin.client-id:}") String clientId,
            @Value("${linkedin.client-secret:}") String clientSecret,
            @Value("${linkedin.redirect-uri:}") String redirectUri) {
        return new LinkedInOidcClient(RestClient.builder().build(), clientId, clientSecret, redirectUri);
    }
}
