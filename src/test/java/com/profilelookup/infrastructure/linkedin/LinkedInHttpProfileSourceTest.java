package com.profilelookup.infrastructure.linkedin;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.profilelookup.domain.ProfileSourceException;
import com.profilelookup.domain.ProfileSourceException.FailureType;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
class LinkedInHttpProfileSourceTest {
    private static LinkedInHttpProfileSource source(UpstreamResponse response) {
        return new LinkedInHttpProfileSource(new FakeGateway(response), new LinkedInProfileMapper(new ObjectMapper()));
    }
    @Test void notFoundIsEmpty() { assertThat(source(new UpstreamResponse(404, "", null)).findByUrl("https://www.linkedin.com/in/nobody")).isEmpty(); }
    @Test void unauthorizedIsClassified() {
        assertThatThrownBy(() -> source(new UpstreamResponse(401, "", null)).findByUrl("https://www.linkedin.com/in/ada"))
            .isInstanceOf(ProfileSourceException.class)
            .extracting(ex -> ((ProfileSourceException) ex).failureType().orElseThrow())
            .isEqualTo(FailureType.UNAUTHENTICATED);
    }
    @Test void rateLimitedIsClassifiedWithRetry() {
        assertThatThrownBy(() -> source(new UpstreamResponse(429, "", "30")).findByUrl("https://www.linkedin.com/in/ada"))
            .isInstanceOf(ProfileSourceException.class)
            .satisfies(ex -> { ProfileSourceException typed = (ProfileSourceException) ex; assertThat(typed.failureType()).contains(FailureType.RATE_LIMITED); assertThat(typed.retryAfter().orElseThrow().toSeconds()).isEqualTo(30); });
    }
    @Test void successBodyIsMapped() {
        assertThat(source(new UpstreamResponse(200, "{\"name\":\"Ada Lovelace\",\"headline\":\"Mathematician\"}", null)).findByUrl("https://www.linkedin.com/in/ada"))
            .get().extracting("name", "headline").containsExactly("Ada Lovelace", "Mathematician");
    }
    private static final class FakeGateway implements LinkedInGateway {
        private final UpstreamResponse response;
        private FakeGateway(UpstreamResponse response) { this.response = response; }
        @Override public UpstreamResponse fetch(String canonicalProfileUrl, String handle) { return response; }
    }
}
