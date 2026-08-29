# linkedin-http-source Specification

## Purpose
Live LinkedIn HTTP adapter behind `ProfileSource`, selected by default.

## Requirements

### Requirement: Default live source
The system SHALL use the LinkedIn HTTP adapter when `profile.source` is
`linkedin` or unset.

#### Scenario: Default wiring
- **WHEN** the application starts without `profile.source` set
- **THEN** the wired `ProfileSource` is `LinkedInHttpProfileSource`

### Requirement: Live miss is 404
#### Scenario: Unknown handle on live source
- **WHEN** live mode is active and the upstream reports not found (or
  maps to empty)
- **THEN** the API returns HTTP 404 problem+json (`profile-not-found`)

### Requirement: Classified upstream failures
#### Scenario: Missing or rejected session
- **WHEN** the session cookie is missing or upstream returns 401/403
- **THEN** the API returns HTTP 503 with type `source-unauthenticated`
