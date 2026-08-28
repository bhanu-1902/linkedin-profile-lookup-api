package com.profilelookup.infrastructure.oidc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;

/**
 * The only class in this project that talks to LinkedIn over the
 * network. Direct calls via Spring's {@code RestClient} -- no
 * spring-boot-starter-oauth2-client, see design.md, Decisions, for why.
 *
 * Every method here either succeeds with the data the caller asked for,
 * or throws {@link LinkedInProviderFailureException}. It never returns a
 * partial or null-riddled result for the caller to accidentally treat as
 * success.
 */
public class LinkedInOidcClient {

    private static final String AUTHORIZATION_ENDPOINT = "https://www.linkedin.com/oauth/v2/authorization";
    private static final String TOKEN_ENDPOINT = "https://www.linkedin.com/oauth/v2/accessToken";
    private static final String USERINFO_ENDPOINT = "https://api.linkedin.com/v2/userinfo";

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public LinkedInOidcClient(RestClient restClient, String clientId, String clientSecret, String redirectUri) {
        if (clientId == null || clientId.isBlank()
                || clientSecret == null || clientSecret.isBlank()
                || redirectUri == null || redirectUri.isBlank()) {
            throw new IllegalStateException(
                    "profile.source=oidc requires LINKEDIN_CLIENT_ID, LINKEDIN_CLIENT_SECRET, and "
                            + "LINKEDIN_REDIRECT_URI to all be set. See .env.example.");
        }
        this.restClient = restClient;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    public String buildAuthorizationUrl(String state) {
        return UriComponentsBuilder.fromUriString(AUTHORIZATION_ENDPOINT)
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state)
                .queryParam("scope", "openid profile")
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();
    }

    public String exchangeCodeForToken(String authorizationCode) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", authorizationCode);
        form.add("redirect_uri", redirectUri);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        TokenResponse response;
        try {
            response = restClient.post()
                    .uri(TOKEN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);
        } catch (RestClientException e) {
            throw new LinkedInProviderFailureException("LinkedIn token exchange failed", e);
        }

        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            throw new LinkedInProviderFailureException("LinkedIn token exchange returned no access token");
        }
        return response.accessToken();
    }

    public LinkedInUserInfo fetchUserInfo(String accessToken) {
        UserInfoResponse response;
        try {
            response = restClient.get()
                    .uri(USERINFO_ENDPOINT)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(UserInfoResponse.class);
        } catch (RestClientException e) {
            throw new LinkedInProviderFailureException("LinkedIn userinfo request failed", e);
        }

        if (response == null || response.sub() == null || response.sub().isBlank()) {
            throw new LinkedInProviderFailureException("LinkedIn userinfo returned a malformed response");
        }
        return new LinkedInUserInfo(response.sub(), response.name(), response.picture());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TokenResponse(@JsonProperty("access_token") String accessToken) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record UserInfoResponse(String sub, String name, String picture) {
    }
}
