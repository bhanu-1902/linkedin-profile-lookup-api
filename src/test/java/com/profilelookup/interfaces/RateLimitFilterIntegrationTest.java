package com.profilelookup.interfaces;

import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scenario: Caller exceeds the request rate --
 * specs/challenge-submission-readiness/spec.md, "Public evaluator access."
 *
 * Isolated in its own {@code @SpringBootTest} context (a fresh
 * {@code RateLimitFilter} bean, so a fresh, empty bucket map) rather than
 * sharing {@code ProfileControllerIntegrationTest}'s context: buckets are
 * keyed by caller address, every {@code TestRestTemplate} call in this JVM
 * shares that same address, and a low capacity here would otherwise starve
 * whichever other test happened to run in the same context afterward.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@TestPropertySource(properties = {
        "ratelimit.capacity=2",
        "ratelimit.refill-period-seconds=60"
})
class RateLimitFilterIntegrationTest {

    @LocalServerPort
    private int port;

    private final TestRestTemplate rest = new TestRestTemplate();

    @Test
    void exceedingRateLimitReturns429WithRetryAfter() {
        String url = "http://localhost:" + port + "/v1/profile?url=https://www.linkedin.com/in/example-profile";

        rest.exchange(url, HttpMethod.GET, null, String.class); // 1
        rest.exchange(url, HttpMethod.GET, null, String.class); // 2 (capacity=2)
        ResponseEntity<String> third = rest.exchange(url, HttpMethod.GET, null, String.class);

        assertThat(third.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(third.getHeaders().get("Retry-After")).isNotNull();
    }
}
