package com.profilelookup.infrastructure.linkedin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.profilelookup.domain.Profile;
import com.profilelookup.domain.ProfileSourceException;
import com.profilelookup.domain.ProfileSourceException.FailureType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LinkedInProfileMapper {
    private final ObjectMapper mapper;
    public LinkedInProfileMapper(ObjectMapper mapper) { this.mapper = mapper; }
    public Optional<Profile> map(String canonicalUrl, String rawBody) {
        if (rawBody == null || rawBody.isBlank()) return Optional.empty();
        final JsonNode root;
        try { root = mapper.readTree(rawBody); }
        catch (Exception e) { throw new ProfileSourceException("Upstream body was not usable JSON", FailureType.UPSTREAM_ERROR); }
        if (root.isMissingNode() || root.isNull() || (root.isObject() && root.isEmpty())) return Optional.empty();
        String name = firstNonBlank(textAt(root, "name"), join(textAt(root, "firstName"), textAt(root, "lastName")));
        String headline = firstNonBlank(textAt(root, "headline"), textAt(root, "title"));
        String location = firstNonBlank(textAt(root, "location"), textAt(root, "geoLocationName"), textAt(root, "geo", "full"));
        String about = firstNonBlank(textAt(root, "about"), textAt(root, "summary"));
        if (name == null && headline == null && about == null) {
            throw new ProfileSourceException("Upstream JSON did not contain a profile payload", FailureType.UPSTREAM_ERROR);
        }
        return Optional.of(Profile.builder(canonicalUrl).name(name).headline(headline).location(location).about(about)
                .experience(mapExperience(root.get("experience"), root.get("positions")))
                .education(mapEducation(root.get("education")))
                .skills(stringList(root.get("skills")))
                .certifications(stringList(root.get("certifications")))
                .languages(stringList(root.get("languages")))
                .profileImageUrls(stringList(firstNode(root.get("profileImageUrls"), root.get("profilePictures"))))
                .build());
    }
    private List<Profile.Experience> mapExperience(JsonNode... candidates) {
        JsonNode arr = firstArray(candidates);
        if (arr == null) return List.of();
        List<Profile.Experience> out = new ArrayList<>();
        for (JsonNode n : arr) {
            out.add(new Profile.Experience(firstNonBlank(textAt(n, "title"), textAt(n, "role")),
                    firstNonBlank(textAt(n, "organization"), textAt(n, "companyName"), textAt(n, "company")),
                    firstNonBlank(textAt(n, "startDate"), textAt(n, "start")),
                    firstNonBlank(textAt(n, "endDate"), textAt(n, "end")), textAt(n, "description")));
        }
        return out;
    }
    private List<Profile.Education> mapEducation(JsonNode node) {
        if (node == null || !node.isArray()) return List.of();
        List<Profile.Education> out = new ArrayList<>();
        for (JsonNode n : node) {
            out.add(new Profile.Education(firstNonBlank(textAt(n, "institution"), textAt(n, "schoolName")),
                    textAt(n, "degree"), firstNonBlank(textAt(n, "fieldOfStudy"), textAt(n, "field")),
                    firstNonBlank(textAt(n, "startDate"), textAt(n, "start")), firstNonBlank(textAt(n, "endDate"), textAt(n, "end"))));
        }
        return out;
    }
    private List<String> stringList(JsonNode node) {
        if (node == null || node.isNull()) return List.of();
        if (node.isTextual()) return List.of(node.asText());
        if (!node.isArray()) return List.of();
        List<String> out = new ArrayList<>();
        for (JsonNode n : node) {
            if (n.isTextual()) out.add(n.asText());
            else if (n.hasNonNull("name")) out.add(n.get("name").asText());
        }
        return out;
    }
    private static JsonNode firstArray(JsonNode... nodes) {
        for (JsonNode n : nodes) { if (n != null && n.isArray()) return n; }
        return null;
    }
    private static JsonNode firstNode(JsonNode... nodes) {
        for (JsonNode n : nodes) { if (n != null && !n.isNull()) return n; }
        return null;
    }
    private static String textAt(JsonNode root, String... path) {
        JsonNode n = root;
        for (String p : path) { if (n == null) return null; n = n.get(p); }
        if (n == null || n.isNull() || !n.isValueNode()) return null;
        String v = n.asText();
        return v == null || v.isBlank() ? null : v;
    }
    private static String join(String a, String b) {
        if (a == null && b == null) return null;
        if (a == null) return b;
        if (b == null) return a;
        return (a + " " + b).trim();
    }
    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) { if (v != null && !v.isBlank()) return v; }
        return null;
    }
}
