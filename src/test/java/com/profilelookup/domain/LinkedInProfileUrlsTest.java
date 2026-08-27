package com.profilelookup.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class LinkedInProfileUrlsTest {

    @Test
    void canonicalizesADifferentlyCasedHostAndDropsQueryAndTrailingSlash() {
        assertThat(LinkedInProfileUrls.canonicalize("https://WWW.LinkedIn.com/in/alex-example/?trk=nav"))
                .contains("https://www.linkedin.com/in/alex-example");
    }

    @Test
    void acceptsTheBareApexDomainWithoutWww() {
        assertThat(LinkedInProfileUrls.canonicalize("https://linkedin.com/in/alex-example"))
                .isPresent();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "not-a-url",
            "http://www.linkedin.com/in/alex-example", // http, not https
            "https://evil-linkedin.com/in/alex-example", // wrong host
            "https://www.linkedin.com/company/example", // wrong path shape
            "https://www.linkedin.com/in/", // empty handle
            "https://www.linkedin.com/in/has a space",
            ""
    })
    void rejectsAnythingThatIsNotACleanProfileUrl(String badUrl) {
        assertThat(LinkedInProfileUrls.canonicalize(badUrl)).isEmpty();
    }

    @Test
    void rejectsNull() {
        assertThat(LinkedInProfileUrls.canonicalize(null)).isEmpty();
    }
}
