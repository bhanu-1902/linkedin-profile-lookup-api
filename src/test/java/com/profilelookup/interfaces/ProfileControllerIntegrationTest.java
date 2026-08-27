package com.profilelookup.interfaces;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end: real HTTP calls through the real filter chain (API key,
 * rate limit) into the real controller, running the default "fixture"
 * profile. Each test below is one scenario from
 * openspec/changes/add-profile-lookup/specs/profile-lookup/spec.md --
 * the mapping is deliberate, not coincidental.
 *
 * Rate limit capacity is overridden to 2 so the 429 scenario is
 * deterministic in a handful of requests rather than needing 20+.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "profile.api-keys=test-key",
        "ratelimit.capacity=2",
        "ratelimit.refill-period-seconds=60"
})
class ProfileControllerIntegrationTest {

    @LocalServerPort
    private int port;

    private final TestRestTemplate rest = new TestRestTemplate();

    private String urlFor(String path) {
        return "http://localhost:" + port + path;
    }

    private HttpEntity<Void> withApiKey() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", "test-key");
        return new HttpEntity<>(headers);
    }

    // Scenario: Known profile returned
    @Test
    void knownFixtureProfileReturns200WithExpectedFields() {
        ResponseEntity<String> response = rest.exchange(
                urlFor("/v1/profile?url=https://www.linkedin.com/in/example-profile"),
                HttpMethod.GET, withApiKey(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"name\":\"Alex Example\"");
    }

    // Scenario: Malformed URL rejected
    @Test
    void malformedUrlReturns400ProblemJson() {
        ResponseEntity<String> response = rest.exchange(
                urlFor("/v1/profile?url=not-a-linkedin-url"),
                HttpMethod.GET, withApiKey(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getHeaders().getContentType().toString()).contains("problem+json");
    }

    // Scenario: Profile not present in any configured source
    @Test
    void wellFormedButUnknownUrlReturns501WithStableProblemType() {
        ResponseEntity<String> response = rest.exchange(
                urlFor("/v1/profile?url=https://www.linkedin.com/in/not-in-fixtures"),
                HttpMethod.GET, withApiKey(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
        assertThat(response.getBody()).contains("no-live-data-source");
    }

    // Scenario: Missing API key
    @Test
    void missingApiKeyReturns401() {
        ResponseEntity<String> response = rest.getForEntity(
                urlFor("/v1/profile?url=https://www.linkedin.com/in/example-profile"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // Scenario: Invalid API key
    @Test
    void wrongApiKeyReturns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", "not-the-right-key");

        ResponseEntity<String> response = rest.exchange(
                urlFor("/v1/profile?url=https://www.linkedin.com/in/example-profile"),
                HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // Scenario: Caller exceeds their rate limit
    @Test
    void exceedingRateLimitReturns429WithRetryAfter() {
        String url = urlFor("/v1/profile?url=https://www.linkedin.com/in/example-profile");

        rest.exchange(url, HttpMethod.GET, withApiKey(), String.class); // 1
        rest.exchange(url, HttpMethod.GET, withApiKey(), String.class); // 2 (capacity=2)
        ResponseEntity<String> third = rest.exchange(url, HttpMethod.GET, withApiKey(), String.class);

        assertThat(third.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(third.getHeaders().get("Retry-After")).isNotNull();
    }
}
