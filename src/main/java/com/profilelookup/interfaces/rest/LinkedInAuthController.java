package com.profilelookup.interfaces.rest;

import com.profilelookup.domain.LinkedInProfileUrls;
import com.profilelookup.domain.Profile;
import com.profilelookup.infrastructure.oidc.LinkedInOidcClient;
import com.profilelookup.infrastructure.oidc.LinkedInOidcProfileSource;
import com.profilelookup.infrastructure.oidc.LinkedInUserInfo;
import com.profilelookup.infrastructure.oidc.OAuthStateStore;
import com.profilelookup.interfaces.rest.dto.LinkedInCallbackResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * "Sign In with LinkedIn" via OIDC -- lets a caller prove they are who
 * they say they are and have their own name/photo become servable
 * through the existing {@code /v1/profile} endpoint. This is
 * structurally incapable of looking up anyone else's profile: LinkedIn's
 * userinfo response only ever describes whoever just completed the
 * consent screen. See openspec/changes/add-oidc-self-lookup/proposal.md,
 * "Why," for the full scope statement.
 *
 * Only registered when {@code profile.source=oidc}; the fixture and stub
 * deployments never expose these endpoints and never need LinkedIn
 * credentials.
 */
@RestController
@RequestMapping("/v1/auth/linkedin")
@ConditionalOnProperty(name = "profile.source", havingValue = "oidc")
public class LinkedInAuthController {

    private final OAuthStateStore stateStore;
    private final LinkedInOidcClient oidcClient;
    private final LinkedInOidcProfileSource oidcProfileSource;

    public LinkedInAuthController(
            OAuthStateStore stateStore, LinkedInOidcClient oidcClient, LinkedInOidcProfileSource oidcProfileSource) {
        this.stateStore = stateStore;
        this.oidcClient = oidcClient;
        this.oidcProfileSource = oidcProfileSource;
    }

    @Operation(summary = "Start LinkedIn OIDC sign-in",
            description = "Redirects to LinkedIn's consent screen. The caller asserts the LinkedIn profile "
                    + "URL their own consented data will be served under, since LinkedIn's OIDC response "
                    + "carries no public profile URL of its own. Never looks up a third party.")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirect to LinkedIn's authorization endpoint"),
            @ApiResponse(responseCode = "400",
                    description = "profileUrl is missing or not a valid LinkedIn profile URL")
    })
    @GetMapping("/login")
    public ResponseEntity<Void> login(@RequestParam(required = false) String profileUrl) {
        String canonicalUrl = LinkedInProfileUrls.canonicalize(profileUrl)
                .orElseThrow(() -> new InvalidProfileUrlException(profileUrl));

        String state = stateStore.issue(canonicalUrl);
        URI authorizationUrl = URI.create(oidcClient.buildAuthorizationUrl(state));

        return ResponseEntity.status(HttpStatus.FOUND).location(authorizationUrl).build();
    }

    @Operation(summary = "Handle the LinkedIn OIDC callback",
            description = "Exchanges the authorization code for the caller's own basic profile and makes it "
                    + "servable via GET /v1/profile for its exact URL only -- never a substitute for any "
                    + "other profile.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consent completed; the profile is now servable"),
            @ApiResponse(responseCode = "400",
                    description = "The state parameter was invalid/expired, or consent was declined"),
            @ApiResponse(responseCode = "502",
                    description = "LinkedIn's token exchange or userinfo call failed")
    })
    @GetMapping("/callback")
    public ResponseEntity<LinkedInCallbackResponse> callback(
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error) {

        // State validation comes first, before even looking at error/code,
        // so a callback with an invalid state never triggers a token
        // exchange -- see spec.md, "Reject callbacks that fail state
        // validation."
        String profileUrl = stateStore.consume(state)
                .orElseThrow(LinkedInInvalidStateException::new);

        if (error != null && !error.isBlank()) {
            throw new LinkedInConsentDeclinedException(error);
        }
        if (code == null || code.isBlank()) {
            throw new LinkedInConsentDeclinedException("missing_authorization_code");
        }

        String accessToken = oidcClient.exchangeCodeForToken(code);
        LinkedInUserInfo userInfo = oidcClient.fetchUserInfo(accessToken);

        Profile profile = Profile.builder(profileUrl)
                .name(userInfo.name())
                .profileImageUrls(userInfo.picture() == null ? List.of() : List.of(userInfo.picture()))
                .build();
        oidcProfileSource.recordConsentedProfile(profile);

        return ResponseEntity.ok(new LinkedInCallbackResponse(
                "ok", profileUrl, "Consent completed. GET /v1/profile?url=" + profileUrl + " now serves this profile."));
    }
}
