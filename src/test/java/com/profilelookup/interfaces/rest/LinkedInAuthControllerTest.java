package com.profilelookup.interfaces.rest;

import com.profilelookup.infrastructure.oidc.LinkedInOidcClient;
import com.profilelookup.infrastructure.oidc.LinkedInOidcProfileSource;
import com.profilelookup.infrastructure.oidc.LinkedInProviderFailureException;
import com.profilelookup.infrastructure.oidc.OAuthStateStore;
import com.profilelookup.interfaces.rest.dto.LinkedInCallbackResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class LinkedInAuthControllerTest {

    private static final String CONSENTED_URL = "https://www.linkedin.com/in/consented-user";

    private OAuthStateStore stateStore;
    private MockRestServiceServer server;
    private LinkedInOidcProfileSource profileSource;
    private LinkedInAuthController controller;

    @BeforeEach
    void setUp() {
        stateStore = new OAuthStateStore();
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        LinkedInOidcClient oidcClient = new LinkedInOidcClient(
                builder.build(), "client-id", "client-secret", "http://localhost:8080/v1/auth/linkedin/callback");
        profileSource = new LinkedInOidcProfileSource();
        controller = new LinkedInAuthController(stateStore, oidcClient, profileSource);
    }

    @Test
    void loginRedirectsToLinkedInWithExpectedQueryParameters() {
        ResponseEntity<Void> response = controller.login(CONSENTED_URL);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        URI location = response.getHeaders().getLocation();
        assertThat(location).isNotNull();
        assertThat(location.toString()).startsWith("https://www.linkedin.com/oauth/v2/authorization");
        String query = location.getQuery();
        assertThat(query).contains("response_type=code");
        assertThat(query).contains("client_id=client-id");
        assertThat(query).contains("scope=openid");
        assertThat(query).contains("state=");
    }

    @Test
    void loginRejectsAMissingOrInvalidProfileUrl() {
        assertThatThrownBy(() -> controller.login("not-a-linkedin-url"))
                .isInstanceOf(InvalidProfileUrlException.class);
        assertThatThrownBy(() -> controller.login(null))
                .isInstanceOf(InvalidProfileUrlException.class);
    }

    @Test
    void callbackCompletesTheFlowAndRecordsTheConsentedProfile() {
        String state = stateStore.issue(CONSENTED_URL);
        server.expect(requestTo("https://www.linkedin.com/oauth/v2/accessToken"))
                .andRespond(withSuccess("{\"access_token\":\"tok-abc\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.linkedin.com/v2/userinfo"))
                .andRespond(withSuccess(
                        "{\"sub\":\"1\",\"name\":\"Alex Example\",\"picture\":\"https://example.com/pic.jpg\"}",
                        MediaType.APPLICATION_JSON));

        ResponseEntity<LinkedInCallbackResponse> response = controller.callback(state, "auth-code", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().linkedInUrl()).isEqualTo(CONSENTED_URL);
        assertThat(profileSource.findByUrl(CONSENTED_URL))
                .isPresent()
                .get()
                .satisfies(profile -> assertThat(profile.name()).isEqualTo("Alex Example"));
    }

    @Test
    void callbackRejectsAnUnrecognizedState() {
        assertThatThrownBy(() -> controller.callback("never-issued", "auth-code", null))
                .isInstanceOf(LinkedInInvalidStateException.class);
    }

    @Test
    void callbackRejectsAnAlreadyConsumedState() {
        String state = stateStore.issue(CONSENTED_URL);
        stateStore.consume(state);

        assertThatThrownBy(() -> controller.callback(state, "auth-code", null))
                .isInstanceOf(LinkedInInvalidStateException.class);
    }

    @Test
    void callbackTreatsAnErrorParameterAsDeclinedConsentNotAProviderFailure() {
        String state = stateStore.issue(CONSENTED_URL);

        assertThatThrownBy(() -> controller.callback(state, null, "user_cancelled_login"))
                .isInstanceOf(LinkedInConsentDeclinedException.class);
        assertThat(profileSource.findByUrl(CONSENTED_URL)).isEmpty();
    }

    @Test
    void callbackTreatsAMissingCodeWithNoErrorAsDeclinedConsent() {
        String state = stateStore.issue(CONSENTED_URL);

        assertThatThrownBy(() -> controller.callback(state, null, null))
                .isInstanceOf(LinkedInConsentDeclinedException.class);
    }

    @Test
    void callbackTokenExchangeFailureIsAProviderFailureAndRecordsNoProfile() {
        String state = stateStore.issue(CONSENTED_URL);
        server.expect(requestTo("https://www.linkedin.com/oauth/v2/accessToken"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> controller.callback(state, "auth-code", null))
                .isInstanceOf(LinkedInProviderFailureException.class);
        assertThat(profileSource.findByUrl(CONSENTED_URL)).isEmpty();
    }

    @Test
    void callbackUserInfoFailureIsAProviderFailureAndRecordsNoProfile() {
        String state = stateStore.issue(CONSENTED_URL);
        server.expect(requestTo("https://www.linkedin.com/oauth/v2/accessToken"))
                .andRespond(withSuccess("{\"access_token\":\"tok-abc\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.linkedin.com/v2/userinfo"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> controller.callback(state, "auth-code", null))
                .isInstanceOf(LinkedInProviderFailureException.class);
        assertThat(profileSource.findByUrl(CONSENTED_URL)).isEmpty();
    }
}
