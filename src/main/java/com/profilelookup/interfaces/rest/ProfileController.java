package com.profilelookup.interfaces.rest;

import com.profilelookup.application.ProfileLookupService;
import com.profilelookup.domain.LinkedInProfileUrls;
import com.profilelookup.domain.Profile;
import com.profilelookup.interfaces.rest.dto.ProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Value;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    private final String profileSource;
    public ProfileController(ProfileLookupService profileLookupService, @Value("${profile.source:linkedin}") String profileSource) {
        this.profileLookupService = profileLookupService;
        this.profileSource = profileSource;
    }

    @Operation(summary = "Look up a profile by URL",
            description = "Returns structured profile data for a LinkedIn profile URL when the configured "
                    + "profile source has it. See README, 'Known limitations,' for which sources are "
                    + "configured in this deployment.")
    @Parameter(name = "url", required = true,
            description = "A LinkedIn profile URL, e.g. https://www.linkedin.com/in/<handle>")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile found; returns the available fields, "
                    + "omitting any that are not present"),
            @ApiResponse(responseCode = "400",
                    description = "url is missing or not a syntactically valid LinkedIn profile URL"),
            @ApiResponse(responseCode = "429",
                    description = "The caller exceeded the per-address request rate limit, or the configured "
                            + "source is itself being rate-limited by its provider",
                    headers = @Header(name = "Retry-After", schema = @Schema(type = "integer"),
                            description = "Seconds to wait before retrying")),
            @ApiResponse(responseCode = "404",
                    description = "Live source is configured but no profile was available for this URL"),
            @ApiResponse(responseCode = "501",
                    description = "No live data source is configured for this profile URL"),
            @ApiResponse(responseCode = "502",
                    description = "The configured source returned a response it could not use"),
            @ApiResponse(responseCode = "503",
                    description = "The configured source is unavailable or could not authenticate with its "
                            + "provider")
    })
    @GetMapping("/profile")
    public ProfileResponse getProfile(@RequestParam(required = false) String url) {
        String canonicalUrl = LinkedInProfileUrls.canonicalize(url)
                .orElseThrow(() -> new InvalidProfileUrlException(url));

        Profile profile = profileLookupService.lookup(canonicalUrl)
                .orElseThrow(() -> missingProfile(canonicalUrl));

        return ProfileResponse.from(profile);
    }
    private RuntimeException missingProfile(String canonicalUrl) {
        if ("linkedin".equals(profileSource)) return new ProfileNotFoundException(canonicalUrl);
        return new ProfileNotAvailableException(canonicalUrl);
    }
}
