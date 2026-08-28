package com.profilelookup.infrastructure.linkedin;

import com.profilelookup.domain.ProfileSourceException;
import com.profilelookup.domain.ProfileSourceException.FailureType;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class RestLinkedInGateway implements LinkedInGateway {
    private final RestClient restClient;
    private final LinkedInProperties properties;
    public RestLinkedInGateway(RestClient restClient, LinkedInProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }
    @Override
    public UpstreamResponse fetch(String canonicalProfileUrl, String handle) {
        if (!properties.hasSessionCookie()) {
            throw new ProfileSourceException("LinkedIn session cookie is not configured", FailureType.UNAUTHENTICATED);
        }
        if (!properties.hasProfileUrlTemplate()) {
            throw new ProfileSourceException("linkedin.profile-url-template is not configured", FailureType.UNAVAILABLE);
        }
        String url = properties.getProfileUrlTemplate().replace("{handle}", handle);
        try {
            return restClient.get()
                    .uri(url)
                    .header("User-Agent", properties.getUserAgent())
                    .header("Cookie", properties.getSessionCookie())
                    .headers(headers -> {
                        if (properties.getCsrfToken() != null && !properties.getCsrfToken().isBlank()) {
                            headers.set("Csrf-Token", properties.getCsrfToken());
                            headers.set("X-Requested-With", "XMLHttpRequest");
                        }
                    })
                    .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_HTML)
                    .exchange((request, response) -> {
                        String body = response.bodyTo(String.class);
                        return new UpstreamResponse(response.getStatusCode().value(), body == null ? "" : body, response.getHeaders().getFirst("Retry-After"));
                    });
        } catch (RestClientException ex) {
            throw new ProfileSourceException("LinkedIn gateway is unreachable", FailureType.UNAVAILABLE);
        }
    }
}
