package com.profilelookup.interfaces.rest;

import com.profilelookup.application.ProfileLookupService;
import com.profilelookup.domain.LinkedInProfileUrls;
import com.profilelookup.domain.Profile;
import com.profilelookup.interfaces.rest.dto.ProfileResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Interface layer: translates HTTP <-> the use case. Knows about
 * {@code @GetMapping} and {@code ProfileResponse}; knows nothing about
 * fixtures, stubs, or how a lookup is actually performed.
 *
 * URL validation delegates entirely to {@link LinkedInProfileUrls}
 * rather than a Bean Validation {@code @Pattern} regex -- one shared,
 * unit-testable definition of "valid LinkedIn profile URL" instead of a
 * regex here and separate string-munging in the fixture adapter. The
 * param is {@code required = false} so a missing {@code url} and an
 * empty/malformed one produce the same problem+json response through
 * one code path, rather than Spring's default missing-parameter error
 * bypassing GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/v1")
public class ProfileController {

    private final ProfileLookupService profileLookupService;

    public ProfileController(ProfileLookupService profileLookupService) {
        this.profileLookupService = profileLookupService;
    }

    @GetMapping("/profile")
    public ProfileResponse getProfile(@RequestParam(required = false) String url) {
        String canonicalUrl = LinkedInProfileUrls.canonicalize(url)
                .orElseThrow(() -> new InvalidProfileUrlException(url));

        Profile profile = profileLookupService.lookup(canonicalUrl)
                .orElseThrow(() -> new ProfileNotAvailableException(canonicalUrl));

        return ProfileResponse.from(profile);
    }
}
