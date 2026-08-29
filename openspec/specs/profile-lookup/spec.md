# profile-lookup Specification

## Purpose
Lets a caller submit a LinkedIn profile URL and receive that profile's
data as structured JSON from the configured `ProfileSource` (default:
live LinkedIn HTTP adapter).

## Requirements

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
The system SHALL NOT silently fabricate a profile for a syntactically
valid URL the configured source cannot resolve. It SHALL return a typed,
documented problem+json response whose status depends on the source mode.

#### Scenario: Live source miss
- **WHEN** the configured source is the live LinkedIn adapter and a
  syntactically valid profile URL has no available profile
- **THEN** the system returns HTTP 404 with a problem+json body whose
  `type` identifies the profile-not-found condition

#### Scenario: Non-live source miss
- **WHEN** the configured source is fixture or stub and a syntactically
  valid profile URL is not available from that source
- **THEN** the system returns HTTP 501 with a problem+json body whose
  `type` identifies the no-live-data-source condition

### Requirement: Rate limiting
The system SHALL limit the number of requests a single caller address may
make within a rolling time window, and SHALL communicate the limit to
callers who exceed it. No credential is required to call the endpoint.

#### Scenario: Caller exceeds their rate limit
- **WHEN** a caller's request rate from their address exceeds the
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
