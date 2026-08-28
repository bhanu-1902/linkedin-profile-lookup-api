## Purpose

Lets a user prove, via LinkedIn's own consent screen, that they are who
they say they are, and have their own basic profile become servable
through the existing profile-lookup endpoint — and only their own, never
anyone else's.

## ADDED Requirements

### Requirement: Initiate LinkedIn sign-in
The system SHALL provide an endpoint that redirects the caller to
LinkedIn's OIDC authorization screen, including a freshly generated,
single-use state value the system will later verify on callback. The
caller SHALL assert the LinkedIn profile URL the consented data will be
served under, since LinkedIn's OIDC response contains no public-profile
identifier of its own.

#### Scenario: Caller starts the sign-in flow
- **WHEN** a caller requests the login endpoint with a syntactically
  valid LinkedIn profile URL
- **THEN** the system responds with a redirect to LinkedIn's real
  authorization endpoint, including a client identifier, redirect URI,
  requested scopes, and a state value unique to this attempt

#### Scenario: Login requested without a valid profile URL
- **WHEN** a caller requests the login endpoint with a missing or
  malformed `profileUrl`
- **THEN** the system returns HTTP 400 with a problem+json body and does
  not contact LinkedIn or issue a state value

### Requirement: Handle a valid callback and store the consented profile
The system SHALL, upon receiving a callback whose state value matches
one it issued and has not yet consumed, exchange the authorization code
for an access token, retrieve the authenticated user's basic profile,
and make that profile available to subsequent lookups for its exact
profile URL.

#### Scenario: Valid callback completes the flow
- **WHEN** LinkedIn redirects back with a valid, previously-issued state
  and a genuine authorization code
- **THEN** the system exchanges the code, retrieves the profile, and a
  subsequent lookup for that exact profile URL returns that data

### Requirement: Reject callbacks that fail state validation
The system SHALL reject any callback whose state value is missing,
unrecognized, or already consumed, and SHALL NOT attempt a token
exchange for such a callback.

#### Scenario: Callback with an unrecognized state
- **WHEN** a callback arrives with a state value the system did not
  issue, or one it already consumed
- **THEN** the system returns a 400 problem+json response and performs
  no token exchange

### Requirement: Honest handling of consent decline and provider failure
The system SHALL distinguish, in its response, between a user declining
consent and the provider or network failing, and SHALL NOT represent
either as a successful lookup.

#### Scenario: User declines consent
- **WHEN** LinkedIn redirects back carrying an error parameter instead
  of an authorization code
- **THEN** the system returns a typed problem+json response identifying
  that consent was declined, distinct from a provider failure

#### Scenario: Token exchange or profile retrieval fails
- **WHEN** the code-for-token exchange or the subsequent profile
  retrieval call fails (network error, non-2xx response, malformed
  payload)
- **THEN** the system returns a 502 problem+json response, and does not
  store a partial or fabricated profile

### Requirement: Serve only the consented profile, never a substitute
The system SHALL treat every profile URL other than the one obtained
through a completed, valid consent flow as unavailable, using the same
signal the system already uses for any profile it has no data for.

#### Scenario: Lookup for a URL that did not just consent
- **WHEN** a caller requests a profile URL other than the one most
  recently obtained through a completed OIDC flow on this instance
- **THEN** the system responds identically to how it responds for any
  other profile it has no live data source for
