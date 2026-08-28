package com.profilelookup.infrastructure.linkedin;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "linkedin")
public class LinkedInProperties {
    private String profileUrlTemplate = "";
    private String sessionCookie = "";
    private String csrfToken = "";
    private String userAgent = "Mozilla/5.0";
    private int timeoutMs = 8000;
    public String getProfileUrlTemplate() { return profileUrlTemplate; }
    public void setProfileUrlTemplate(String v) { this.profileUrlTemplate = v; }
    public String getSessionCookie() { return sessionCookie; }
    public void setSessionCookie(String v) { this.sessionCookie = v; }
    public String getCsrfToken() { return csrfToken; }
    public void setCsrfToken(String v) { this.csrfToken = v; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String v) { this.userAgent = v; }
    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int v) { this.timeoutMs = v; }
    public boolean hasSessionCookie() { return sessionCookie != null && !sessionCookie.isBlank(); }
    public boolean hasProfileUrlTemplate() { return profileUrlTemplate != null && !profileUrlTemplate.isBlank(); }
}
