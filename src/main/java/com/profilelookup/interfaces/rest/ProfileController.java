package com.profilelookup.interfaces.rest;

import com.profilelookup.application.ProfileLookupService;
import com.profilelookup.domain.Profile;
import com.profilelookup.interfaces.rest.dto.ProfileResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Interface layer: translates HTTP <-> the use case. Knows about
 * {@code @GetMapping} and {@code ProfileResponse}; knows nothing about
 * fixtures, stubs, or how a lookup is actually performed.
 *
 * {@code @Validated} at the class level (not just {@code @Valid} on a
 * body) is what makes Bean Validation annotations on a
 * {@code @RequestParam} actually run.
 */
@RestController
@RequestMapping("/v1")
@Validated
public class ProfileController {

    private static final String LINKEDIN_PROFILE_URL_PATTERN =
            "^https://(www\\.)?linkedin\\.com/in/[a-zA-Z0-9\\-_%]+/?$";

    private final ProfileLookupService profileLookupService;

    public ProfileController(ProfileLookupService profileLookupService) {
        this.profileLookupService = profileLookupService;
    }

    @GetMapping("/profile")
    public ProfileResponse getProfile(
            @RequestParam
            @NotBlank(message = "url is required")
            @Pattern(
                    regexp = LINKEDIN_PROFILE_URL_PATTERN,
                    message = "url must look like https://www.linkedin.com/in/<handle>")
            String url) {

        Profile profile = profileLookupService.lookup(url)
                .orElseThrow(() -> new ProfileNotAvailableException(url));

        return ProfileResponse.from(profile);
    }
}
