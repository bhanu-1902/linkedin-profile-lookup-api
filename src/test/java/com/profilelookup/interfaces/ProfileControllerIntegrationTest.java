package com.profilelookup.interfaces;

import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end against the optional fixture source so CI needs no LinkedIn
 * cookie. Runtime default remains {@code linkedin}; these scenarios pin
 * {@code profile.source=fixture}. Each test maps to a scenario in
 * openspec/specs/profile-lookup/spec.md.
 *
 * The rate-limit-exceeded scenario lives in
 * {@code RateLimitFilterIntegrationTest}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@TestPropertySource(properties = "profile.source=fixture")
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
