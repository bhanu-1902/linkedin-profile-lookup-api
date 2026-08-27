package com.profilelookup.interfaces.rest;

/**
 * Thrown by the controller when {@code LinkedInProfileUrls.canonicalize}
 * rejects the submitted value -- covers missing, blank, and malformed
 * URLs with one exception type instead of Bean Validation's
 * ConstraintViolationException plus Spring's separate
 * MissingServletRequestParameterException for the "absent" case.
 */
public class InvalidProfileUrlException extends RuntimeException {

    private final String submittedValue;

    public InvalidProfileUrlException(String submittedValue) {
        super("Not a valid LinkedIn profile URL: " + submittedValue);
        this.submittedValue = submittedValue;
    }

    public String getSubmittedValue() {
        return submittedValue;
    }
}
