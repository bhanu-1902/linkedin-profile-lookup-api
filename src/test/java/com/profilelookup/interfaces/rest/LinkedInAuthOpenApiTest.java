package com.profilelookup.interfaces.rest;

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
 * Verifies the generated OpenAPI document actually describes the two new
 * LinkedIn OIDC endpoints, including the error responses documented in
 * openspec/changes/add-oidc-self-lookup/tasks.md, 4.3 and 4.4.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@TestPropertySource(properties = {
        "profile.source=oidc",
        "linkedin.client-id=test-client",
        "linkedin.client-secret=test-secret",
        "linkedin.redirect-uri=http://localhost:8080/v1/auth/linkedin/callback"
})
class LinkedInAuthOpenApiTest {

    @LocalServerPort
    private int port;

    private final TestRestTemplate rest = new TestRestTemplate();

    @Test
    void openApiDocumentDescribesBothEndpointsAndTheirErrorResponses() {
        ResponseEntity<String> response =
                rest.getForEntity("http://localhost:" + port + "/v3/api-docs", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        assertThat(body).contains("/v1/auth/linkedin/login");
        assertThat(body).contains("/v1/auth/linkedin/callback");
        assertThat(body).contains("\"302\"");
        assertThat(body).contains("\"400\"");
        assertThat(body).contains("\"502\"");
    }
}
