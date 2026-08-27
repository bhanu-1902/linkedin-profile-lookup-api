package com.profilelookup.domain;

import java.util.List;

/**
 * The domain entity. Deliberately framework-free -- no Spring, no
 * Jackson annotations, no javax/jakarta imports. This is what "the
 * domain has no framework imports" (Clean Architecture's Dependency
 * Rule) looks like in practice: this file compiles with plain javac
 * and nothing else.
 *
 * Immutable (Builder pattern, see {@link Builder}) because a Profile
 * is a snapshot, not something callers should mutate in place.
 */
public final class Profile {

    private final String linkedInUrl;
    private final String name;
    private final String headline;
    private final String location;
    private final String about;
    private final List<Experience> experience;
    private final List<Education> education;
    private final List<String> skills;
    private final List<String> certifications;
    private final List<String> languages;
    private final List<String> profileImageUrls;

    private Profile(Builder b) {
        this.linkedInUrl = b.linkedInUrl;
        this.name = b.name;
        this.headline = b.headline;
        this.location = b.location;
        this.about = b.about;
        this.experience = List.copyOf(b.experience);
        this.education = List.copyOf(b.education);
        this.skills = List.copyOf(b.skills);
        this.certifications = List.copyOf(b.certifications);
        this.languages = List.copyOf(b.languages);
        this.profileImageUrls = List.copyOf(b.profileImageUrls);
    }

    public String linkedInUrl() { return linkedInUrl; }
    public String name() { return name; }
    public String headline() { return headline; }
    public String location() { return location; }
    public String about() { return about; }
    public List<Experience> experience() { return experience; }
    public List<Education> education() { return education; }
    public List<String> skills() { return skills; }
    public List<String> certifications() { return certifications; }
    public List<String> languages() { return languages; }
    public List<String> profileImageUrls() { return profileImageUrls; }

    public static Builder builder(String linkedInUrl) {
        return new Builder(linkedInUrl);
    }

    /** One entry in a profile's experience section. */
    public record Experience(
            String title,
            String organization,
            String startDate,
            String endDate,
            String description) {
    }

    /** One entry in a profile's education section. */
    public record Education(
            String institution,
            String degree,
            String fieldOfStudy,
            String startDate,
            String endDate) {
    }

    /**
     * Builder, per the requirement that fields the source data doesn't
     * have (e.g. no "about" section) are simply absent rather than
     * forcing a large constructor with nulls threaded through it.
     */
    public static final class Builder {
        private final String linkedInUrl;
        private String name = "";
        private String headline = "";
        private String location = "";
        private String about = "";
        private List<Experience> experience = List.of();
        private List<Education> education = List.of();
        private List<String> skills = List.of();
        private List<String> certifications = List.of();
        private List<String> languages = List.of();
        private List<String> profileImageUrls = List.of();

        private Builder(String linkedInUrl) {
            this.linkedInUrl = linkedInUrl;
        }

        public Builder name(String v) { this.name = v; return this; }
        public Builder headline(String v) { this.headline = v; return this; }
        public Builder location(String v) { this.location = v; return this; }
        public Builder about(String v) { this.about = v; return this; }
        public Builder experience(List<Experience> v) { this.experience = v; return this; }
        public Builder education(List<Education> v) { this.education = v; return this; }
        public Builder skills(List<String> v) { this.skills = v; return this; }
        public Builder certifications(List<String> v) { this.certifications = v; return this; }
        public Builder languages(List<String> v) { this.languages = v; return this; }
        public Builder profileImageUrls(List<String> v) { this.profileImageUrls = v; return this; }

        public Profile build() {
            return new Profile(this);
        }
    }
}
