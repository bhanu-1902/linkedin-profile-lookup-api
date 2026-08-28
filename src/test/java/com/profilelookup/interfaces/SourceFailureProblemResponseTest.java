package com.profilelookup.interfaces;

import com.profilelookup.domain.ProfileSourceException;
import com.profilelookup.infrastructure.FaultyProfileSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives a real HTTP request through the real controller and
 * {@code GlobalExceptionHandler} for each classified
 * {@link ProfileSourceException.FailureType}, using {@link FaultyProfileSource}
 * -- a test-only {@code ProfileSource} -- to trigger each one on demand. This
 * is the executable form of
 * openspec/changes/prepare-live-profile-source/tasks.md, 3.2: "map each
 * classified source failure to a stable RFC 9457 response... verify this
 * with a test-only source implementation."
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(SourceFailureProblemResponseTest.FaultySourceConfig.class)
class SourceFailureProblemResponseTest {

    @TestConfiguration
    static class FaultySourceConfig {
        @Bean
        @Primary
        FaultyProfileSource faultyProfileSource() {
            return new FaultyProfileSource();
        }
    }

    @Autowired
    private FaultyProfileSource faultyProfileSource;

    @LocalServerPort
    private int port;

    private final TestRestTemplate rest = new TestRestTemplate();

    private String profileUrl() {
        return "http://localhost:" + port + "/v1/profile?url=https://www.linkedin.com/in/example-profile";
    }

    @Test
    void unauthenticatedSourceReturns503WithStableProblemType() {
        faultyProfileSource.failNextWith(ProfileSourceException.FailureType.UNAUTHENTICATED);

        ResponseEntity<String> response = rest.getForEntity(profileUrl(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).contains("source-unauthenticated");
    }

    @Test
    void unavailableSourceReturns503WithStableProblemType() {
        faultyProfileSource.failNextWith(ProfileSourceException.FailureType.UNAVAILABLE);

        ResponseEntity<String> response = rest.getForEntity(profileUrl(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).contains("source-unavailable");
    }

    @Test
    void rateLimitedSourceReturns429WithRetryAfterHeader() {
        faultyProfileSource.failNextWith(ProfileSourceException.FailureType.RATE_LIMITED, Duration.ofSeconds(42));

        ResponseEntity<String> response = rest.getForEntity(profileUrl(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).contains("source-rate-limited");
        assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("42");
    }

    @Test
    void upstreamErrorReturns502WithStableProblemType() {
        faultyProfileSource.failNextWith(ProfileSourceException.FailureType.UPSTREAM_ERROR);

        ResponseEntity<String> response = rest.getForEntity(profileUrl(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).contains("source-upstream-error");
    }
}
