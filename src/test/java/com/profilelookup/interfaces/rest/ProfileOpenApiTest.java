package com.profilelookup.interfaces.rest;

import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the generated OpenAPI document actually describes
 * {@code GET /v1/profile}'s parameter and every response the deployed
 * fixture endpoint can serve -- see
 * openspec/changes/prepare-live-profile-source/tasks.md, 4.1, and
 * specs/challenge-submission-readiness/spec.md, "Complete published API
 * contract."
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class ProfileOpenApiTest {

    @LocalServerPort
    private int port;

    private final TestRestTemplate rest = new TestRestTemplate();

    @Test
    void openApiDocumentDescribesTheProfileEndpointAndEveryResponseCode() {
        ResponseEntity<String> response =
                rest.getForEntity("http://localhost:" + port + "/v3/api-docs", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        assertThat(body).contains("/v1/profile");
        assertThat(body).contains("\"200\"");
        assertThat(body).contains("\"400\"");
        assertThat(body).contains("\"429\"");
        assertThat(body).contains("\"501\"");
        assertThat(body).contains("\"502\"");
        assertThat(body).contains("\"503\"");
        assertThat(body).contains("Retry-After");
    }
}
