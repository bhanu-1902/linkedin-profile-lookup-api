package com.profilelookup.infrastructure.oidc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

class LinkedInOidcClientTest {

    private MockRestServiceServer server;
    private LinkedInOidcClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new LinkedInOidcClient(
                builder.build(), "client-id", "client-secret", "http://localhost:8080/v1/auth/linkedin/callback");
    }

    @Test
    void rejectsBlankCredentialsAtConstructionRatherThanFailingLater() {
        RestClient restClient = RestClient.builder().build();

        assertThatThrownBy(() -> new LinkedInOidcClient(restClient, "", "secret", "http://localhost/callback"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new LinkedInOidcClient(restClient, "id", null, "http://localhost/callback"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new LinkedInOidcClient(restClient, "id", "secret", " "))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void buildsAuthorizationUrlWithAllRequiredParameters() {
        String url = client.buildAuthorizationUrl("state-123");

        assertThat(url).startsWith("https://www.linkedin.com/oauth/v2/authorization");
        assertThat(url).contains("response_type=code");
        assertThat(url).contains("client_id=client-id");
        assertThat(url).contains("state=state-123");
        assertThat(url).contains("scope=openid");
    }

    @Test
    void exchangesAuthorizationCodeForAnAccessToken() {
        server.expect(requestTo("https://www.linkedin.com/oauth/v2/accessToken"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"tok-abc\",\"expires_in\":3600}", MediaType.APPLICATION_JSON));

        String token = client.exchangeCodeForToken("auth-code");

        assertThat(token).isEqualTo("tok-abc");
    }

    @Test
    void tokenExchangeNon2xxResponseBecomesProviderFailure() {
        server.expect(requestTo("https://www.linkedin.com/oauth/v2/accessToken"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.exchangeCodeForToken("auth-code"))
                .isInstanceOf(LinkedInProviderFailureException.class);
    }

    @Test
    void tokenResponseMissingAccessTokenBecomesProviderFailure() {
        server.expect(requestTo("https://www.linkedin.com/oauth/v2/accessToken"))
                .andRespond(withSuccess("{\"expires_in\":3600}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.exchangeCodeForToken("auth-code"))
                .isInstanceOf(LinkedInProviderFailureException.class);
    }

    @Test
    void fetchesUserInfoWithTheAccessToken() {
        server.expect(requestTo("https://api.linkedin.com/v2/userinfo"))
                .andRespond(withSuccess(
                        "{\"sub\":\"abc123\",\"name\":\"Alex Example\",\"picture\":\"https://example.com/pic.jpg\"}",
                        MediaType.APPLICATION_JSON));

        LinkedInUserInfo info = client.fetchUserInfo("tok-abc");

        assertThat(info.sub()).isEqualTo("abc123");
        assertThat(info.name()).isEqualTo("Alex Example");
        assertThat(info.picture()).isEqualTo("https://example.com/pic.jpg");
    }

    @Test
    void userInfoNon2xxResponseBecomesProviderFailure() {
        server.expect(requestTo("https://api.linkedin.com/v2/userinfo"))
                .andRespond(withUnauthorizedRequest());

        assertThatThrownBy(() -> client.fetchUserInfo("bad-token"))
                .isInstanceOf(LinkedInProviderFailureException.class);
    }

    @Test
    void userInfoResponseMissingSubBecomesProviderFailure() {
        server.expect(requestTo("https://api.linkedin.com/v2/userinfo"))
                .andRespond(withSuccess("{\"name\":\"No Sub\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetchUserInfo("tok-abc"))
                .isInstanceOf(LinkedInProviderFailureException.class);
    }
}
