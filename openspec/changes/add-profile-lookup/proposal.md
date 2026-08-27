## Why

A take-home brief asks for a hosted API that accepts a LinkedIn profile URL
and returns that profile's data as structured JSON. LinkedIn provides no
sanctioned API for fetching a third party's profile by URL — the only
self-serve product (Sign In with LinkedIn / OIDC) returns data solely for
the authenticated caller. Any implementation that fulfills the literal
brief therefore requires automated, credential-based scraping, which
LinkedIn's User Agreement §8.2 prohibits and has litigated aggressively
(hiQ Labs v. LinkedIn; LinkedIn Corp. v. Nubela/Proxycurl, 2025).

This change builds the honest version instead: a production-shaped API
with the data-source access point isolated behind a single interface, so
the system demonstrates every architectural and engineering skill the
brief is actually evaluating — without shipping a ToS-violating scraper.

## What Changes

- New `GET /v1/profile` endpoint accepting a `url` query parameter,
  returning a structured profile as JSON.
- New `ProfileSource` port (interface) isolating profile retrieval from
  the rest of the system.
- New `FixtureProfileSource` adapter, serving profile data seeded from the
  requester's own official LinkedIn data export (a legitimate, first-party
  source — see LinkedIn's "Get a copy of your data").
- New `StubProfileSource` adapter, returning a typed, documented
  "no live data source configured" response (RFC 9457 problem+json) for
  any URL not present in fixtures — explicitly, not a silent scraper.
- New API-key authentication (single header, plain servlet filter — no
  Spring Security, see design.md for why).
- New rate limiting (token bucket per API key).
- New OpenAPI 3.1 documentation, auto-generated and served interactively.
- **Explicitly out of scope**: any adapter that authenticates to LinkedIn
  and retrieves arbitrary third-party profile data. See design.md,
  "Rejected approach."

## Capabilities

### New Capabilities
- `profile-lookup`: accept a LinkedIn profile URL and return that
  profile's available data as structured JSON, sourced only from adapters
  that do not violate LinkedIn's terms of service.

### Modified Capabilities
(none — this is the first capability in this project)

## Impact

- **New code**: domain (`Profile`, `ProfileSource`), application
  (`ProfileLookupService`), infrastructure (`FixtureProfileSource`,
  `StubProfileSource`, fixture loader), interfaces (REST controller, DTOs,
  API-key filter, rate-limit filter, global exception handler).
- **New dependencies**: Spring Boot 4.1.x (web, validation, actuator),
  springdoc-openapi (OpenAPI 3.1 + Scalar UI), Bucket4j core (rate
  limiting).
- **No external systems** are called at runtime by any shipped adapter —
  fixtures are static JSON derived from a legitimately-exported LinkedIn
  archive.
- **Deployment**: containerized, deployable to Render (or any container
  host) over HTTPS.
