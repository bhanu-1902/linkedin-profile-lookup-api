package com.profilelookup.infrastructure.linkedin;
public record UpstreamResponse(int status, String body, String retryAfterHeader) {
    public boolean isNotFound() { return status == 404; }
    public boolean isUnauthorized() { return status == 401 || status == 403; }
    public boolean isRateLimited() { return status == 429; }
    public boolean isServerError() { return status >= 500; }
    public boolean isSuccess() { return status >= 200 && status < 300; }
}
