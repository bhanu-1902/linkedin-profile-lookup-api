## 1. Project setup

- [ ] 1.1 Create Maven project (pom.xml, wrapper) targeting Spring Boot
      4.1.x / Java 17+ and verify `./mvnw -v` resolves the wrapper
- [ ] 1.2 Add dependencies (web, validation, actuator, springdoc-openapi,
      bucket4j-core, test) and verify `./mvnw dependency:resolve` succeeds
- [ ] 1.3 Add `.gitignore`, `.env.example`, and Dockerfile

## 2. Domain layer

- [x] 2.1 Implement `Profile` entity and verify it compiles standalone
      with `javac` (no framework imports — this is the layer's whole
      point). Verified repeatedly, including after the 2.3 addition.
- [x] 2.2 Implement `ProfileSource` port and the domain-level
      "not available" signal, verified the same way
- [x] 2.3 Implement `LinkedInProfileUrls` (canonicalization/validation)
      as a framework-free domain utility shared by the controller and
      `FixtureProfileSource`, replacing a duplicated regex-in-controller
      / string-munging-in-adapter split. Verified the same way, plus
      a dedicated unit test (untested by a real build — see note below).

## 3. Application layer

- [ ] 3.1 Implement `ProfileLookupService` use case against the
      `ProfileSource` port and verify it depends on no concrete adapter

## 4. Infrastructure layer

- [ ] 4.1 Implement `FixtureProfileSource`, loading from a bundled JSON
      fixture, and verify it satisfies the `ProfileSource` contract test
- [ ] 4.2 Implement `StubProfileSource`, returning the documented
      "no live data source" signal for any URL, and verify it satisfies
      the same contract test
- [ ] 4.3 Wire adapter selection via `profile.source` property
      (`@ConditionalOnProperty`) and verify both profiles boot
      successfully

## 5. Interface layer

- [ ] 5.1 Implement `ProfileController` (`GET /v1/profile`) with request
      validation and verify a `MockMvc` test hits it end-to-end
- [ ] 5.2 Implement the API-key `OncePerRequestFilter` and verify
      requests without a valid key are rejected with 401
- [ ] 5.3 Implement the rate-limit filter (Bucket4j) and verify a burst
      of requests past the configured limit returns 429 with
      `Retry-After`
- [ ] 5.4 Implement the global `@RestControllerAdvice` returning
      RFC 9457 `ProblemDetail` for every error case named in the spec
      and verify each scenario in `specs/profile-lookup/spec.md` maps to
      a passing test

## 6. Documentation & deployment

- [ ] 6.1 Configure springdoc-openapi and verify `/v3/api-docs` and the
      Scalar/Swagger UI serve successfully on a local run
- [ ] 6.2 Write the README (architecture, decisions, known limitations
      including the legal reasoning, quickstart) and verify the
      quickstart steps work from a clean clone
- [ ] 6.3 Verify the Docker image builds and the container serves
      `/actuator/health`

## 7. Archive

- [ ] 7.1 Run `openspec archive add-profile-lookup` once all tasks above
      are checked and verify the capability lands in `openspec/specs/`

## 8. Remediation round (post-review fixes)

Prompted by a real local build attempt catching what this environment
could not (no Maven Central access here — see design.md addendum
below). All items in this group are written and internally consistent,
but share the same "needs a real `mvn`/`docker` run to confirm" status
as everything above; none are checked off on the strength of code
review alone.

- [ ] 8.1 Fix illegal `--` inside XML comments in `pom.xml` and verify
      with a real XML parser (done: `xml.etree.ElementTree` confirms
      well-formed) *and* with `mvn` itself actually parsing the POM
- [ ] 8.2 Make absent profile fields serialize as omitted, not `""`
      (`spring.jackson.default-property-inclusion=non_null` +
      nullable `Profile` scalar fields) and verify via the new
      `absentFieldsAreOmittedFromTheJsonBodyEntirely` integration test
- [ ] 8.3 Fix integration-test rate-limit isolation (one API key per
      test instead of one shared key) and verify the full suite passes
      regardless of JUnit's method execution order
- [ ] 8.4 Add the Maven Wrapper, fetched verbatim from the
      `apache/maven-wrapper` project's own tagged release rather than
      reproduced from memory, and verify `./mvnw -v` on a real machine
- [ ] 8.5 Add a CI workflow (test, package, container build, OpenSpec
      `validate --strict --all`) and verify it goes green on a real push
- [ ] 8.6 Confirm whether `com.fasterxml.jackson.databind` resolves
      against the pinned Spring Boot 4.1.1 / springdoc 3.0.3 versions,
      or whether Jackson 3's `tools.jackson` namespace is required —
      genuinely open, needs a real compiler error message to resolve,
      not further guessing

