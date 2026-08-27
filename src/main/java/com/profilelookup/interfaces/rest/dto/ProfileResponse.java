package com.profilelookup.interfaces.rest.dto;

import com.profilelookup.domain.Profile;

import java.util.List;

/**
 * Response DTO. A record, not a Builder: this is a straight 1:1 field
 * copy from the domain entity with no optional-field construction
 * problem to solve, so a second Builder here would be the "redundant
 * presenter/DTO pass-through" the design doc explicitly warns against.
 * Builder earns its place once, in {@link Profile}, where it actually
 * has a job to do.
 */
public record ProfileResponse(
        String linkedInUrl,
        String name,
        String headline,
        String location,
        String about,
        List<ExperienceResponse> experience,
        List<EducationResponse> education,
        List<String> skills,
        List<String> certifications,
        List<String> languages,
        List<String> profileImageUrls) {

    public record ExperienceResponse(
            String title, String organization, String startDate, String endDate, String description) {
    }

    public record EducationResponse(
            String institution, String degree, String fieldOfStudy, String startDate, String endDate) {
    }

    public static ProfileResponse from(Profile profile) {
        return new ProfileResponse(
                profile.linkedInUrl(),
                profile.name(),
                profile.headline(),
                profile.location(),
                profile.about(),
                profile.experience().stream()
                        .map(e -> new ExperienceResponse(
                                e.title(), e.organization(), e.startDate(), e.endDate(), e.description()))
                        .toList(),
                profile.education().stream()
                        .map(e -> new EducationResponse(
                                e.institution(), e.degree(), e.fieldOfStudy(), e.startDate(), e.endDate()))
                        .toList(),
                profile.skills(),
                profile.certifications(),
                profile.languages(),
                profile.profileImageUrls());
    }
}
