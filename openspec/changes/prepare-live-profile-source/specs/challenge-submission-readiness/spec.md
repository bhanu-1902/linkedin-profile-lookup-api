## Purpose

Ensures the profile-lookup repository can be built, evaluated, and deployed
for the challenge while the decision on its eventual live profile source is
pending.

## ADDED Requirements

### Requirement: Reproducible challenge build
The repository SHALL build and run its automated test suite with the documented
Maven Wrapper command on a Java 17-or-newer environment, without requiring an
IDE-specific setup or an uncommitted local dependency.

#### Scenario: Clean checkout verification
- **WHEN** a reviewer runs the documented wrapper test command from a clean
  checkout using a supported JDK
- **THEN** compilation and all automated tests complete successfully

### Requirement: Public evaluator access
The deployed challenge endpoint SHALL be callable without a private or
undisclosed credential, and SHALL apply a bounded request rate limit to the
caller identity available to the service.

#### Scenario: Evaluator requests a fixture profile
- **WHEN** a caller requests a known fixture profile through the deployed
  endpoint without supplying an API key
- **THEN** the service returns HTTP 200 with the documented profile response

#### Scenario: Evaluator exceeds the request rate
- **WHEN** a caller exceeds the configured request limit
- **THEN** the service returns HTTP 429 with a `Retry-After` header and a
  problem+json response

### Requirement: Source readiness without live integration
The service SHALL keep its profile-source boundary independent of the HTTP API
and SHALL expose stable, non-sensitive API errors for source availability and
provider failures. Until a live source is explicitly approved and configured,
the deployed service SHALL use only fixture data and SHALL make no runtime
calls to LinkedIn.

#### Scenario: Fixture does not contain a valid profile URL
- **WHEN** a caller submits a syntactically valid LinkedIn profile URL that is
  absent from the fixture source
- **THEN** the service returns a documented problem+json response identifying
  that no live profile source is configured, without exposing internal
  configuration or credentials

#### Scenario: A source reports a retryable provider failure
- **WHEN** a configured profile source reports a retryable upstream failure
- **THEN** the HTTP API returns a documented problem+json response with a
  stable error type and, when known, a retry interval

### Requirement: Complete published API contract
The service SHALL publish an OpenAPI document and interactive documentation
that describe the profile request parameter, profile response, and all
documented error responses served by the challenge deployment.

#### Scenario: Reviewer inspects API documentation
- **WHEN** a reviewer requests the published OpenAPI document
- **THEN** it describes HTTP 200, 400, 429, 501, and provider-failure
responses, including the `Retry-After` header where applicable
