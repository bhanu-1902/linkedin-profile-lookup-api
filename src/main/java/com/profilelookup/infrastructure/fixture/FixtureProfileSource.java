package com.profilelookup.infrastructure.fixture;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.profilelookup.domain.LinkedInProfileUrls;
import com.profilelookup.domain.Profile;
import com.profilelookup.domain.ProfileSource;
import com.profilelookup.domain.ProfileSourceException;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Adapter: serves profiles loaded once at startup from a bundled JSON
 * fixture file, derived from a legitimate first-party source (the
 * requester's own LinkedIn data export -- see the fixture file's own
 * header comment). Never calls out to LinkedIn or anywhere else.
 *
 * Deliberately uses only {@code ObjectMapper.readValue(..., Class)} --
 * Jackson's most fundamental, longest-stable API -- rather than the tree
 * model, since Spring Boot 4's move to Jackson 3 changed which mapper
 * bean gets auto-configured. Binding straight to records sidesteps that
 * entirely: no dependency on which mapper Spring wires up, because this
 * class builds its own.
 */
public class FixtureProfileSource implements ProfileSource {

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record FixtureFile(List<FixtureProfile> profiles) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record FixtureProfile(
            String linkedInUrl,
            String name,
            String headline,
            String location,
            String about,
            List<FixtureExperience> experience,
            List<FixtureEducation> education,
            List<String> skills,
            List<String> certifications,
            List<String> languages,
            List<String> profileImageUrls) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record FixtureExperience(
            String title, String organization, String startDate, String endDate, String description) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record FixtureEducation(
            String institution, String degree, String fieldOfStudy, String startDate, String endDate) {
    }

    private final Map<String, Profile> profilesByUrl = new HashMap<>();

    public FixtureProfileSource(Resource fixtureResource) {
        loadFixtures(fixtureResource);
    }

    @Override
    public Optional<Profile> findByUrl(String linkedInUrl) {
        // Defensive: the controller always canonicalizes before calling
        // here, but this adapter re-canonicalizes with the SAME shared
        // utility rather than its own normalize() logic, so there is
        // exactly one definition of "canonical LinkedIn URL" in the
        // whole codebase, not one in the controller and a second one
        // here that could quietly drift apart from it.
        return LinkedInProfileUrls.canonicalize(linkedInUrl)
                .map(profilesByUrl::get);
    }

    private void loadFixtures(Resource fixtureResource) {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = fixtureResource.getInputStream()) {
            FixtureFile file = mapper.readValue(in, FixtureFile.class);
            for (FixtureProfile fp : file.profiles()) {
                Profile profile = toProfile(fp);
                String canonicalUrl = LinkedInProfileUrls.canonicalize(profile.linkedInUrl())
                        .orElseThrow(() -> new ProfileSourceException(
                                "Fixture entry has an invalid linkedInUrl: " + profile.linkedInUrl()));
                profilesByUrl.put(canonicalUrl, profile);
            }
        } catch (IOException e) {
            throw new ProfileSourceException(
                    "Could not load fixture file: " + fixtureResource.getDescription(), e);
        }
    }

    private Profile toProfile(FixtureProfile fp) {
        // Scalars pass through as-is: a missing/null field in the source
        // data means the response omits it, not "" -- see spec.md,
        // "Known profile returned": "omitting any field that is not
        // present," and application.yml's
        // spring.jackson.default-property-inclusion=non_null, which is
        // what actually drops null fields from the JSON body.
        return Profile.builder(fp.linkedInUrl())
                .name(fp.name())
                .headline(fp.headline())
                .location(fp.location())
                .about(fp.about())
                .experience(fp.experience() == null ? List.of() : fp.experience().stream()
                        // e.g. a current role's endDate is null on purpose --
                        // that's "present," not "unknown," and the response
                        // should say so by omitting the field, not by
                        // printing an empty string.
                        .map(e -> new Profile.Experience(
                                e.title(), e.organization(), e.startDate(), e.endDate(), e.description()))
                        .toList())
                .education(fp.education() == null ? List.of() : fp.education().stream()
                        .map(e -> new Profile.Education(
                                e.institution(), e.degree(), e.fieldOfStudy(), e.startDate(), e.endDate()))
                        .toList())
                .skills(orEmptyList(fp.skills()))
                .certifications(orEmptyList(fp.certifications()))
                .languages(orEmptyList(fp.languages()))
                .profileImageUrls(orEmptyList(fp.profileImageUrls()))
                .build();
    }

    private List<String> orEmptyList(List<String> l) {
        return l == null ? List.of() : l;
    }
}

