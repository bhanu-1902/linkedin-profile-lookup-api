package com.profilelookup.interfaces.rest;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end through the real filter chain -- proves the OIDC login and
 * callback endpoints are reachable without an X-API-Key header. A direct
 * unit test of {@link LinkedInAuthController} would not catch a
 * regression here, since it never goes through {@link ApiKeyAuthFilter}
 * at all; only a real HTTP round trip does. See ApiKeyAuthFilter's
 * javadoc for why these two endpoints are excluded from the API-key
 * gate.
 *
 * Uses the JDK's own {@link HttpClient} with redirects explicitly
 * disabled, rather than {@code TestRestTemplate} (which follows
 * redirects by default) -- this test must observe the raw 302 without
 * ever actually following it to the real linkedin.com.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "profile.source=oidc",
        "linkedin.client-id=test-client",
        "linkedin.client-secret=test-secret",
        "linkedin.redirect-uri=http://localhost:8080/v1/auth/linkedin/callback"
})
class LinkedInAuthEndpointsIntegrationTest {

    @LocalServerPort
    private int port;

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @Test
    void loginIsReachableWithoutAnApiKeyBecauseItIsBrowserFacing() throws IOException, InterruptedException {
        HttpResponse<Void> response = http.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port
                        + "/v1/auth/linkedin/login?profileUrl=https://www.linkedin.com/in/example-profile"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.discarding());

        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(response.headers().firstValue("Location"))
                .hasValueSatisfying(location ->
                        assertThat(location).startsWith("https://www.linkedin.com/oauth/v2/authorization"));
    }

    @Test
    void callbackIsReachableWithoutAnApiKeyBecauseLinkedInCannotSupplyOne() throws IOException, InterruptedException {
        HttpResponse<String> response = http.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port
                        + "/v1/auth/linkedin/callback?state=never-issued&code=whatever"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        // Not 401: the request reached the controller (and correctly
        // failed state validation instead).
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.headers().firstValue("Content-Type"))
                .hasValueSatisfying(contentType -> assertThat(contentType).contains("problem+json"));
    }
}
