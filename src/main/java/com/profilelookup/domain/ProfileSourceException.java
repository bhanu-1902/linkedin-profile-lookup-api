package com.profilelookup.domain;

/**
 * Thrown by a {@link ProfileSource} for a genuine failure (fixture file
 * unreadable, malformed data, etc.) -- distinct from the normal, expected
 * "no profile for this URL" case, which is {@code Optional.empty()}, not
 * an exception.
 */
public class ProfileSourceException extends RuntimeException {

    public ProfileSourceException(String message) {
        super(message);
    }

    public ProfileSourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
