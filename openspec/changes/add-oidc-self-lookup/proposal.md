## Why

The shipped system (`openspec/changes/add-profile-lookup`, archived pending
a verified build) deliberately has no live LinkedIn data path — only a
fixture and an honest stub. That was the right call given what the
brief's literal ask required (credential-based scraping of arbitrary
third parties), but it leaves the project with zero real, working
integrations to point to.

LinkedIn does sanction exactly one thing: "Sign In with LinkedIn" via
OpenID Connect, where a user explicitly consents to sharing their own
basic profile. This change adds that as a third `ProfileSource` adapter.

Scope discipline, stated up front: this does not, and cannot, satisfy
the challenge's core requirement (arbitrary profile URL in, that
profile's data out). The OIDC userinfo endpoint returns only two of the
requested fields (name, picture) and only for whoever just completed the
consent flow — never a third party. This change is worth building
because it's a real, correctly-scoped integration the README can point
to, not because it closes the gap with Tross. See design.md for the
honest accounting of what this does and doesn't buy.

## What Changes

- New endpoints: `GET /v1/auth/linkedin/login` (redirect to LinkedIn's
  consent screen), `GET /v1/auth/linkedin/callback` (handle the
  authorization-code exchange).
- New adapter: `LinkedInOidcProfileSource implements ProfileSource`,
  serving data for exactly one profile — whichever one most recently
  completed the consent flow on this instance — and `Optional.empty()`
  for every other URL, identically to how the stub adapter already
  behaves for everything.
- New `profile.source=oidc` option alongside the existing `fixture` and
  `stub` values.
- CSRF protection on the OAuth callback via a server-issued, single-use
  `state` value — implemented directly, not via Spring Security's OAuth2
  Client support. See design.md, Decisions, for why that's consistent
  with the existing "no Spring Security" call in the first change rather
  than a departure from it.
- The login endpoint requires the caller to assert their own profile URL
  up front (`?profileUrl=`), validated with the same
  `LinkedInProfileUrls` utility the rest of the system already uses.
  This is a technical necessity, not a plan embellishment: LinkedIn's
  OIDC `userinfo` response has no public-profile-URL claim, only an
  opaque `sub`, so there is no other way to know which URL the consented
  profile should be servable under. See design.md, Decisions.
- Explicitly out of scope: anything that looks up a profile other than
  the one that just consented. If a future change wants that, it needs
  its own proposal and its own honest accounting — this one doesn't
  backdoor it in.

## Capabilities

### New Capabilities

- `oidc-self-lookup`: let a user authenticate via LinkedIn OIDC and have
  their own consented profile become servable through the existing
  `/v1/profile` endpoint — and only that profile.

### Modified Capabilities

(none — `profile-lookup`'s existing requirements are already
adapter-agnostic; "Known profile returned" and "Profile not present in
any configured source" both already cover this adapter's behavior
without needing to change their wording.)

## Impact

- **New code**: two controller endpoints, one adapter, one small
  in-memory "currently consented profile" holder (single-instance,
  non-persistent — a stated limitation, not a bug, for a demo
  deployment), one in-memory single-use state store, one LinkedIn OIDC
  HTTP client.
- **New config**: `LINKEDIN_CLIENT_ID`, `LINKEDIN_CLIENT_SECRET`,
  `LINKEDIN_REDIRECT_URI` — requires registering a real LinkedIn
  Developer App (self-serve, unlike the partner-gated products we
  already confirmed are closed to individual applicants).
- **New dependency**: none. Spring's `RestClient` (from
  `spring-boot-starter-web`, already present) covers both outbound calls
  this adapter makes.
- **No change** to the `fixture` or `stub` adapters, or to anything in
  `add-profile-lookup`'s already-written code.
