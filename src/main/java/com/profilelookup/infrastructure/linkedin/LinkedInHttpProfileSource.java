package com.profilelookup.infrastructure.linkedin;
import com.profilelookup.domain.LinkedInProfileUrls;
import com.profilelookup.domain.Profile;
import com.profilelookup.domain.ProfileSource;
import com.profilelookup.domain.ProfileSourceException;
import com.profilelookup.domain.ProfileSourceException.FailureType;
import java.time.Duration;
import java.util.Optional;
public class LinkedInHttpProfileSource implements ProfileSource {
    private final LinkedInGateway gateway;
    private final LinkedInProfileMapper mapper;
    public LinkedInHttpProfileSource(LinkedInGateway gateway, LinkedInProfileMapper mapper) {
        this.gateway = gateway; this.mapper = mapper;
    }
    @Override public Optional<Profile> findByUrl(String linkedInUrl) {
        String canonical = LinkedInProfileUrls.canonicalize(linkedInUrl).orElse(null);
        if (canonical == null) return Optional.empty();
        String handle = canonical.substring(canonical.lastIndexOf('/') + 1);
        UpstreamResponse response = gateway.fetch(canonical, handle);
        if (response.isNotFound()) return Optional.empty();
        if (response.isUnauthorized()) throw new ProfileSourceException("LinkedIn rejected the configured session", FailureType.UNAUTHENTICATED);
        if (response.isRateLimited()) throw new ProfileSourceException("LinkedIn is rate-limiting this source", FailureType.RATE_LIMITED, parseRetryAfter(response.retryAfterHeader()));
        if (response.isServerError()) throw new ProfileSourceException("LinkedIn is unavailable", FailureType.UNAVAILABLE);
        if (!response.isSuccess()) throw new ProfileSourceException("LinkedIn returned an unusable status", FailureType.UPSTREAM_ERROR);
        return mapper.map(canonical, response.body());
    }
    private static Duration parseRetryAfter(String header) {
        if (header == null || header.isBlank()) return Duration.ofSeconds(60);
        try { return Duration.ofSeconds(Math.max(1, Long.parseLong(header.trim()))); }
        catch (NumberFormatException e) { return Duration.ofSeconds(60); }
    }
}
