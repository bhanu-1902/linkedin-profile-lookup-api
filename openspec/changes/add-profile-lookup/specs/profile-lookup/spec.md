## Purpose

Lets a caller submit a LinkedIn profile URL and receive that profile's
publicly-describable data as structured JSON, sourced only through means
that do not violate LinkedIn's terms of service.

## ADDED Requirements

### Requirement: Profile lookup by URL
The system SHALL provide an endpoint that accepts a LinkedIn profile URL
and returns the corresponding profile as structured JSON when the profile
is available through a configured data source.

#### Scenario: Known profile returned
- **WHEN** a caller requests a profile URL that exists in the configured
  data source
- **THEN** the system returns HTTP 200 with a JSON body containing the
  available fields: name, headline, location, about, experience,
  education, skills, certifications, languages, and image references,
  omitting any field that is not present for that profile

#### Scenario: Malformed URL rejected
- **WHEN** a caller submits a value that is not a syntactically valid
  LinkedIn profile URL
- **THEN** the system returns HTTP 400 with a problem+json body
  describing the validation failure, and does not attempt a lookup

### Requirement: Honest handling of unavailable profiles
The system SHALL NOT silently fabricate or omit-without-explanation a
response for a syntactically valid profile URL that the configured data
source cannot resolve. It SHALL instead return a typed, documented
response explaining that no live data source is configured for
unrecognized profiles.

#### Scenario: Profile not present in any configured source
- **WHEN** a caller requests a syntactically valid profile URL that is not
  present in the fixture data source
- **THEN** the system returns a problem+json response with a stable
  `type` identifying the "no live data source" condition, status 501, and
  a `detail` explaining that only fixture-backed profiles are served in
  this deployment

### Requirement: API key authentication
The system SHALL require a valid API key on every `/v1/**` request and
SHALL reject requests missing one or presenting an unrecognized one.

#### Scenario: Missing API key
- **WHEN** a request to a `/v1/**` endpoint carries no `X-API-Key` header
- **THEN** the system returns HTTP 401 with a problem+json body, and does
  not invoke the profile lookup

#### Scenario: Invalid API key
- **WHEN** a request carries an `X-API-Key` header whose value does not
  match a configured key
- **THEN** the system returns HTTP 401 with a problem+json body

### Requirement: Rate limiting
The system SHALL limit the number of requests a single API key may make
within a rolling time window, and SHALL communicate the limit to callers
who exceed it.

#### Scenario: Caller exceeds their rate limit
- **WHEN** a caller's request rate for their API key exceeds the
  configured limit
- **THEN** the system returns HTTP 429 with a `Retry-After` header and a
  problem+json body

### Requirement: Machine-readable API documentation
The system SHALL publish an OpenAPI 3.1 document describing every `/v1/**`
endpoint, and SHALL make it viewable through an interactive documentation
UI.

#### Scenario: Documentation reflects the live contract
- **WHEN** a client requests the published OpenAPI document
- **THEN** the returned document accurately describes the request
  parameters, response schema, and error responses of every `/v1/**`
  endpoint, generated from the same code that serves those endpoints
