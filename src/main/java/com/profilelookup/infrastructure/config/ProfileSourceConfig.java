package com.profilelookup.infrastructure.config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.profilelookup.domain.ProfileSource;
import com.profilelookup.infrastructure.fixture.FixtureProfileSource;
import com.profilelookup.infrastructure.linkedin.LinkedInHttpProfileSource;
import com.profilelookup.infrastructure.linkedin.LinkedInProfileMapper;
import com.profilelookup.infrastructure.linkedin.LinkedInProperties;
import com.profilelookup.infrastructure.linkedin.RestLinkedInGateway;
import com.profilelookup.infrastructure.stub.StubProfileSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
@Configuration
@EnableConfigurationProperties(LinkedInProperties.class)
public class ProfileSourceConfig {
    @Bean
    @ConditionalOnProperty(name = "profile.source", havingValue = "fixture", matchIfMissing = true)
    public ProfileSource fixtureProfileSource(@Value("${profile.fixture-path:classpath:fixtures/sample-profiles.json}") Resource fixtureResource) {
        return new FixtureProfileSource(fixtureResource);
    }
    @Bean
    @ConditionalOnProperty(name = "profile.source", havingValue = "stub")
    public ProfileSource stubProfileSource() { return new StubProfileSource(); }
    @Bean
    @ConditionalOnProperty(name = "profile.source", havingValue = "linkedin")
    public ProfileSource linkedInProfileSource(LinkedInProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getTimeoutMs());
        requestFactory.setReadTimeout(properties.getTimeoutMs());
        RestClient restClient = RestClient.builder().requestFactory(requestFactory).build();
        return new LinkedInHttpProfileSource(new RestLinkedInGateway(restClient, properties), new LinkedInProfileMapper(new ObjectMapper()));
    }
}
