## Why

The current project demonstrates a fixture-backed profile API, but it is not
ready for the hiring challenge: it does not compile, its deployed configuration
is fixture-only, and the generated API key prevents an evaluator from using a
public deployment. Tross has now clarified that the eventual data source must
use direct HTTP calls to LinkedIn endpoints without browser automation; the
exact endpoint and compliance scope remain pending their written response.

This change prepares every other part of the repository so that the later
decision is isolated to one infrastructure adapter and its deployment
configuration, rather than requiring another architectural rewrite.

## What Changes

- Make the Java 17 build, test suite, container build, and CI reproducibly
  executable from a clean clone.
- Make the challenge deployment publicly evaluable without a privately shared
  API key, while retaining bounded, single-instance rate limiting for the
  demo.
- Formalize the profile-source boundary and typed provider failure model so a
  future source can report not-found, authentication, rate-limit, and upstream
  failures without leaking provider details to API callers.
- Make the API contract and OpenAPI document describe every success and error
  response that the deployed service serves.
- Keep the fixture source as the only runtime profile source and keep all
  LinkedIn credentials, session material, endpoint calls, and browser
  automation out of this change.
- Rewrite challenge-facing documentation so it accurately describes the
  current fixture deployment and the remaining live-adapter decision without
  presenting a refusal as the submitted solution.

## Capabilities

### New Capabilities

- `challenge-submission-readiness`: make the profile API buildable, publicly
  evaluable, and explicit about provider availability while a live source is
  awaiting approval.

### Modified Capabilities

(none as a formal delta here — `add-profile-lookup`'s own delta spec was
edited directly to drop the API-key requirement and re-key rate limiting
by caller address, then archived into `openspec/specs/profile-lookup/`,
so that capability's spec already reflects this change's final behavior.
This change's own `challenge-submission-readiness` capability records the
rest of the readiness behavior -- reproducible build, classified source
failures, complete OpenAPI contract -- separately rather than folding it
into `profile-lookup`.)

## Impact

- **Code**: Maven configuration, REST filters and exception handling,
  `ProfileSource` error signaling, Spring configuration, controller OpenAPI
  metadata, and tests.
- **Deployment**: Render blueprint remains fixture-backed, but no longer
  requires an undisclosed generated API key for challenge evaluation.
- **Documentation**: README, OpenAPI, and OpenSpec become aligned with the
  challenge and clearly state that live integration is deferred pending Tross's
  answer.
- **Out of scope**: LinkedIn endpoint discovery, credential/session handling,
  live profile retrieval, browser automation, external-data brokers, and
  creation of a GitHub repository or hosted service account.
