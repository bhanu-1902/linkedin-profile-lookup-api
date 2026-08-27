package com.profilelookup.infrastructure.fixture;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        return Optional.ofNullable(profilesByUrl.get(normalize(linkedInUrl)));
    }

    private void loadFixtures(Resource fixtureResource) {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = fixtureResource.getInputStream()) {
            FixtureFile file = mapper.readValue(in, FixtureFile.class);
            for (FixtureProfile fp : file.profiles()) {
                Profile profile = toProfile(fp);
                profilesByUrl.put(normalize(profile.linkedInUrl()), profile);
            }
        } catch (IOException e) {
            throw new ProfileSourceException(
                    "Could not load fixture file: " + fixtureResource.getDescription(), e);
        }
    }

    private Profile toProfile(FixtureProfile fp) {
        return Profile.builder(fp.linkedInUrl())
                .name(orEmpty(fp.name()))
                .headline(orEmpty(fp.headline()))
                .location(orEmpty(fp.location()))
                .about(orEmpty(fp.about()))
                .experience(fp.experience() == null ? List.of() : fp.experience().stream()
                        .map(e -> new Profile.Experience(
                                orEmpty(e.title()), orEmpty(e.organization()),
                                orEmpty(e.startDate()), orEmpty(e.endDate()), orEmpty(e.description())))
                        .toList())
                .education(fp.education() == null ? List.of() : fp.education().stream()
                        .map(e -> new Profile.Education(
                                orEmpty(e.institution()), orEmpty(e.degree()),
                                orEmpty(e.fieldOfStudy()), orEmpty(e.startDate()), orEmpty(e.endDate())))
                        .toList())
                .skills(orEmptyList(fp.skills()))
                .certifications(orEmptyList(fp.certifications()))
                .languages(orEmptyList(fp.languages()))
                .profileImageUrls(orEmptyList(fp.profileImageUrls()))
                .build();
    }

    private String orEmpty(String s) {
        return s == null ? "" : s;
    }

    private List<String> orEmptyList(List<String> l) {
        return l == null ? List.of() : l;
    }

    /** Strips query string and trailing slash so URL variants match the same fixture. */
    private String normalize(String url) {
        String noQuery = url.split("\\?", 2)[0];
        return noQuery.endsWith("/") ? noQuery.substring(0, noQuery.length() - 1) : noQuery;
    }
}

