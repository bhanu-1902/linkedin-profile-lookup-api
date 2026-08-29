package com.profilelookup.interfaces;

import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end: real HTTP calls through the real filter chain (rate limit
 * only -- there is no credential gate, see
 * openspec/changes/prepare-live-profile-source/design.md, "Remove the
 * challenge API-key gate") into the real controller, running the default
 * "fixture" profile. Each test below is one scenario from
 * openspec/specs/profile-lookup/spec.md -- the
 * mapping is deliberate, not coincidental.
 *
 * The rate-limit-exceeded scenario lives in its own
 * {@code RateLimitFilterIntegrationTest} instead of here: buckets are now
 * keyed by caller address rather than a per-test API key, so every request
 * in this class shares one bucket (the test client's own address) -- a low
 * capacity here would make these functional tests interfere with each
 * other depending on run order, not just the dedicated rate-limit test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class ProfileControllerIntegrationTest {

    @LocalServerPort
    private int port;

    private final TestRestTemplate rest = new TestRestTemplate();

    private String urlFor(String path) {
        return "http://localhost:" + port + path;
    }

    // Scenario: Known profile returned. Also covers "Public evaluator
    // access": a known fixture profile returns 200 with no credential.
    @Test
    void knownFixtureProfileReturns200WithExpectedFields() {
        ResponseEntity<String> response = rest.getForEntity(
                urlFor("/v1/profile?url=https://www.linkedin.com/in/example-profile"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"name\":\"Alex Example\"");
    }

    // Same requirement, the omission half: a profile with missing
    // fields should not print "" or null for them at all.
    @Test
    void absentFieldsAreOmittedFromTheJsonBodyEntirely() {
        ResponseEntity<String> response = rest.getForEntity(
                urlFor("/v1/profile?url=https://www.linkedin.com/in/example-incomplete-profile"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).doesNotContain("\"about\"");
        assertThat(response.getBody()).doesNotContain("\"endDate\":null");
        assertThat(response.getBody()).contains("\"name\":\"Sam Sparse\"");
    }

    // Scenario: Malformed URL rejected
    @Test
    void malformedUrlReturns400ProblemJson() {
        ResponseEntity<String> response = rest.getForEntity(
                urlFor("/v1/profile?url=not-a-linkedin-url"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getHeaders().getContentType().toString()).contains("problem+json");
    }

    // Scenario: Profile not present in any configured source
    @Test
    void wellFormedButUnknownUrlReturns501WithStableProblemType() {
        ResponseEntity<String> response = rest.getForEntity(
                urlFor("/v1/profile?url=https://www.linkedin.com/in/not-in-fixtures"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
        assertThat(response.getBody()).contains("no-live-data-source");
    }

    // Regression: an unmapped route previously fell into the generic 500
    // handler instead of a correct 404. See
    // GlobalExceptionHandler.handleNoResourceFound.
    @Test
    void unmappedRouteReturns404NotAGeneric500() {
        ResponseEntity<String> response = rest.getForEntity(urlFor("/v1/no-such-endpoint"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getHeaders().getContentType().toString()).contains("problem+json");
    }
}
