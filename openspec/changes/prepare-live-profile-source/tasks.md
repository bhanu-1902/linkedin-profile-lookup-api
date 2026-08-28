## 1. Reproducible build

- [x] 1.1 Configure the Maven compiler explicitly for Java 17 records and
  verify `mvnw.cmd -B test` completes successfully on Windows.
- [x] 1.2 Verify the committed wrapper remains usable from Unix CI and update
  the README with wrapper-based commands for Windows and Unix.
- [x] 1.3 Run `mvnw.cmd -B package` and build the Docker image; verify the
  packaged application exposes its health endpoint when run locally.
  (Docker itself isn't installed in this dev environment; verified instead
  by running the packaged jar directly against `/actuator/health`,
  `/v1/profile`, and `/v3/api-docs`. `docker build` runs in CI, unchanged.)

## 2. Public challenge access

- [x] 2.1 Remove the API-key gate and its deployment configuration; verify a
  known fixture profile returns HTTP 200 without an `X-API-Key` header.
- [x] 2.2 Key the existing rate limiter by the caller address and verify a
  burst from one caller returns HTTP 429 with `Retry-After`.
- [x] 2.3 Update the Render blueprint and environment example to match the
  public fixture deployment, and verify no API-key setting remains required.

## 3. Source-boundary readiness

- [x] 3.1 Add a small classified, non-sensitive provider-failure signal to
  the existing `ProfileSource` contract and verify its unit tests distinguish
  absent profile data from retryable source failure.
- [x] 3.2 Map each classified source failure to a stable RFC 9457 response,
  including retry information when present, and verify this with a test-only
  source implementation.
- [x] 3.3 Keep fixture mode as the default and verify the integration suite
  makes no network calls while preserving the documented 501 response for an
  unknown fixture URL.

## 4. Contract and documentation

- [x] 4.1 Add generated OpenAPI metadata for the profile parameter, HTTP 200,
  400, 429, 501, and provider-failure responses; verify `/v3/api-docs`
  contains each response and the retry header where applicable.
- [x] 4.2 Rewrite the README to give a clean-clone quickstart, public fixture
  API examples, architecture, current limitations, and the explicit deferred
  live-adapter decision; verify every documented local command works.
- [x] 4.3 Run `openspec validate --strict --all` and update the CI workflow if
  needed so it validates this change alongside tests, packaging, and the
  container build. (Validates clean: 3/3 changes pass. The existing CI
  workflow already runs `mvnw -B test`, packaging, the container build, and
  `openspec validate --strict --all` on every push -- no change needed.)

## 5. Submission handoff checks

- [x] 5.1 Record the successful local verification results and leave the
  repository ready for a later adapter-only change; verify no source file,
  environment example, test, or README contains LinkedIn credentials,
  session material, or private endpoint details.
- [ ] 5.2 After the user creates the external GitHub and hosting resources,
  verify the public HTTPS URL, health endpoint, fixture lookup, and API docs
  manually before submission. (Not started -- creating the GitHub repo and
  Render deployment are the user's own external actions, out of scope for
  this session.)
