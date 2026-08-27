package com.profilelookup.domain;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The one place that decides what counts as a valid LinkedIn profile
 * URL, and what its canonical form is. Framework-free on purpose --
 * compiles with plain javac -- so both the controller (validation) and
 * any ProfileSource adapter (matching) share exactly one definition
 * instead of a regex in one place and ad hoc string-munging in another.
 *
 * Rules: HTTPS only; host is exactly linkedin.com or www.linkedin.com
 * (case-insensitive); path is exactly /in/{handle}; query string and
 * fragment are dropped; a single trailing slash is collapsed.
 */
public final class LinkedInProfileUrls {

    private static final Pattern HANDLE_PATTERN = Pattern.compile("^[a-zA-Z0-9\\-_%]+$");

    private LinkedInProfileUrls() {
    }

    public static Optional<String> canonicalize(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return Optional.empty();
        }

        URI uri;
        try {
            uri = new URI(rawUrl.trim());
        } catch (URISyntaxException e) {
            return Optional.empty();
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            return Optional.empty();
        }

        String host = uri.getHost();
        if (host == null) {
            return Optional.empty();
        }
        String lowerHost = host.toLowerCase(Locale.ROOT);
        if (!lowerHost.equals("linkedin.com") && !lowerHost.equals("www.linkedin.com")) {
            return Optional.empty();
        }

        String path = uri.getPath();
        if (path == null) {
            return Optional.empty();
        }
        String trimmedPath = path.endsWith("/") && path.length() > 1
                ? path.substring(0, path.length() - 1)
                : path;

        if (!trimmedPath.startsWith("/in/")) {
            return Optional.empty();
        }
        String handle = trimmedPath.substring("/in/".length());
        if (handle.isEmpty() || handle.contains("/") || !HANDLE_PATTERN.matcher(handle).matches()) {
            return Optional.empty();
        }

        return Optional.of("https://" + lowerHost + "/in/" + handle);
    }

    public static boolean isValid(String rawUrl) {
        return canonicalize(rawUrl).isPresent();
    }
}
