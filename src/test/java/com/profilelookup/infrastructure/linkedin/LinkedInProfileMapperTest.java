package com.profilelookup.infrastructure.linkedin;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.profilelookup.domain.Profile;
import com.profilelookup.domain.ProfileSourceException;
import com.profilelookup.domain.ProfileSourceException.FailureType;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
class LinkedInProfileMapperTest {
    private final LinkedInProfileMapper mapper = new LinkedInProfileMapper(new ObjectMapper());
    @Test void mapsFixtureShapedDocument() {
        String json = "{\"name\":\"Ada Lovelace\",\"headline\":\"Mathematician\",\"experience\":[{\"title\":\"Analyst\",\"organization\":\"Babbage\"}],\"skills\":[\"Mathematics\"]}";
        Profile profile = mapper.map("https://www.linkedin.com/in/ada", json).orElseThrow();
        assertThat(profile.name()).isEqualTo("Ada Lovelace");
        assertThat(profile.skills()).containsExactly("Mathematics");
    }
    @Test void mapsFirstAndLastNameWhenNameIsAbsent() {
        Profile profile = mapper.map("https://www.linkedin.com/in/ada", "{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\",\"headline\":\"Mathematician\"}").orElseThrow();
        assertThat(profile.name()).isEqualTo("Ada Lovelace");
    }
    @Test void blankBodyIsEmptyNotAnError() { assertThat(mapper.map("https://www.linkedin.com/in/ada", "  ")).isEmpty(); }
    @Test void garbageJsonIsUpstreamError() {
        assertThatThrownBy(() -> mapper.map("https://www.linkedin.com/in/ada", "<html>nope</html>"))
            .isInstanceOf(ProfileSourceException.class)
            .extracting(ex -> ((ProfileSourceException) ex).failureType().orElseThrow())
            .isEqualTo(FailureType.UPSTREAM_ERROR);
    }
}
