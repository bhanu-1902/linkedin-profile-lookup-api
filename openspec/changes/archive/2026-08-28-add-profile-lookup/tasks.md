## 1. Project setup

- [x] 1.1 Create Maven project (pom.xml, wrapper) targeting Spring Boot
      4.1.x / Java 17+ and verify `./mvnw -v` resolves the wrapper
- [x] 1.2 Add dependencies (web, actuator, springdoc-openapi,
      bucket4j-core, test) and verify `./mvnw dependency:resolve` succeeds
- [x] 1.3 Add `.gitignore`, `.env.example`, and Dockerfile

## 2. Domain layer

- [x] 2.1 Implement `Profile` entity and verify it compiles standalone
      with `javac` (no framework imports — this is the layer's whole
      point)
- [x] 2.2 Implement `ProfileSource` port and the domain-level
      "not available" signal (`Optional.empty()`), verified the same way
- [x] 2.3 Implement `LinkedInProfileUrls` (canonicalization/validation)
      as a framework-free domain utility shared by the controller and
      `FixtureProfileSource`

## 3. Application layer

- [x] 3.1 Implement `ProfileLookupService` use case against the
      `ProfileSource` port and verify it depends on no concrete adapter

## 4. Infrastructure layer

- [x] 4.1 Implement `FixtureProfileSource`, loading from a bundled JSON
      fixture, and verify it satisfies the `ProfileSource` contract test
- [x] 4.2 Implement `StubProfileSource`, returning the documented
      "no live data source" signal for any URL, and verify it satisfies
      the same contract test
- [x] 4.3 Wire adapter selection via `profile.source` property
      (`@ConditionalOnProperty`) and verify both profiles boot
      successfully

## 5. Interface layer

- [x] 5.1 Implement `ProfileController` (`GET /v1/profile`) with request
      validation and verify an integration test hits it end-to-end
- [x] 5.2 Implement the rate-limit filter (Bucket4j), keyed by caller
      address, and verify a burst of requests past the configured limit
      returns 429 with `Retry-After`. (Originally specified as a
      per-API-key filter with a separate API-key gate; both were removed
      by `openspec/changes/prepare-live-profile-source/` so the challenge
      deployment is callable without a private credential — see that
      change's design.md.)
- [x] 5.3 Implement the global `@RestControllerAdvice` returning
      RFC 9457 `ProblemDetail` for every error case named in the spec
      and verify each scenario in `specs/profile-lookup/spec.md` maps to
      a passing test

## 6. Documentation & deployment

- [x] 6.1 Configure springdoc-openapi and verify `/v3/api-docs` and the
      Swagger UI serve successfully on a local run
- [x] 6.2 Write the README (architecture, decisions, known limitations
      including the legal reasoning, quickstart) and verify the
      quickstart steps work from a clean clone
- [x] 6.3 Verify the packaged jar serves `/actuator/health` locally; the
      Docker image build itself is verified in CI
      (`.github/workflows/ci.yml`)

## 7. Archive

- [x] 7.1 Run `openspec archive add-profile-lookup` once all tasks above
      are checked and verify the capability lands in `openspec/specs/`

## Verification history

This change was originally written and reviewed without a real local
build (no Maven Central access in that environment), and its first
"Remediation round" (illegal `--` in a `pom.xml` comment, absent-field
serialization, rate-limit test isolation, the committed Maven Wrapper, a
CI workflow, and a Jackson-namespace question) was resolved before this
file was last rewritten. A subsequent real build (`mvnw.cmd -B clean
test` on a JDK 17+ runtime, plus `-B package` and running the packaged
jar against `/actuator/health`, `/v1/profile`, and `/v3/api-docs`) has
since gone green end-to-end; every item above is checked against that
verified state, not code review alone.
