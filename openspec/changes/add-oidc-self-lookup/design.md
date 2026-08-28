## Context

Builds on `add-profile-lookup`'s architecture: `ProfileSource` is the one
port, adapters are selected by `profile.source`. This adapter slots in
as a third implementation, no port changes needed — see proposal.md for
why the existing `profile-lookup` spec already covers this adapter's
behavior without modification. Target: same Spring Boot 4.1.x / Java 17+
as the rest of the project.

## Goals / Non-Goals

**Goals:**

- A real, working OAuth/OIDC integration the README can point to and a
  reviewer can actually run, not a described-but-unbuilt idea.
- Correct CSRF handling on the callback (the state parameter), since
  this is the one place in the whole project where getting a security
  detail wrong has a real consequence (an attacker could otherwise bind
  a victim's session to an attacker-controlled LinkedIn account).
- Zero new runtime dependencies.

**Non-Goals:**

- Looking up anyone else's profile. Not a limitation to work around —
  the entire point of this adapter is that it can't, structurally,
  because the userinfo endpoint it calls has no such capability.
- Multi-user or persistent storage of consented profiles. One in-memory
  slot, single instance, cleared on restart — adequate for a demo,
  stated plainly rather than quietly outgrown.
- Refresh-token handling or long-lived sessions. The access token is
  used once, immediately, to fetch userinfo, then discarded.

## Decisions

**Hand-rolled OAuth flow via Spring's `RestClient`, not Spring
Security's OAuth2 Client.** This extends, rather than departs from, the
`add-profile-lookup` decision to skip Spring Security entirely (see that
change's design.md). The reasoning is the same: Spring Security 7 ships
with Spring Boot 4, its defaults were the source of the CSRF concern
that motivated the original decision, and its compatibility with this
project's pinned versions is exactly the kind of thing this environment
cannot compile-verify. A single, well-documented OAuth flow (one
authorization redirect, one callback, one token exchange, one userinfo
call) is small enough to implement directly and verify by reading it,
rather than configuring a framework module to behave one specific way.
Alternative considered: `spring-boot-starter-oauth2-client` — rejected
for the same reasons as before, not new ones.

**The login endpoint requires the caller's own `profileUrl` up front.**
LinkedIn's OIDC `userinfo` response (`sub`, `name`, `picture`, `email`,
…) carries no public profile URL — `sub` is an opaque member ID, not a
`/in/{handle}` vanity URL, and LinkedIn does not expose a claim that
maps one to the other via this flow. Since `Profile` and the `/v1/profile`
endpoint are keyed by that URL, something has to supply it, and OIDC
itself cannot. The person completing their own consent flow is the one
person entitled to assert it — this is exactly analogous to a "verify
this is your profile" step, not a lookup of someone else's data. The
value is validated with the same `LinkedInProfileUrls.canonicalize` the
rest of the system already uses, so a malformed assertion is rejected
before LinkedIn is ever contacted. Alternative considered: derive a
synthetic URL from `sub` (e.g. `https://www.linkedin.com/in/oidc-<sub>`)
— rejected because it isn't the person's real profile URL and would
misrepresent what the system actually knows.

**State parameter stored in an in-memory `ConcurrentHashMap`, same
pattern as the existing rate limiter.** Issued on `/login` (alongside
the asserted `profileUrl`), consumed (removed) on a matching
`/callback`, expired after a short window (5 minutes) to bound memory
growth from abandoned flows. Single-instance limitation, documented
rather than solved with a session store or Redis nobody's asked for.

**The "current consented profile" is a single mutable slot, not a
session-keyed map.** A real multi-user product would key this by
session/user; this project has no user accounts at all outside of the
API key, and adding one to support a single demo adapter would be
exactly the over-engineering the original architecture review explicitly
warned against. The slot is overwritten by whoever completes the flow
most recently — fine for a solo demo, and said so in the README, not
left for a reviewer to discover the hard way.

**Errors distinguish "user declined" from "provider failed."** These are
different failure modes with different meanings (a business decision by
the user vs. an infrastructure problem) and collapsing them into one
generic error would lose information a real caller might reasonably
want — consistent with the RFC 9457 discipline already established for
the rest of this API. Both return 400, but with distinct `type` URIs
(`consent-declined` vs `invalid-oauth-state`), and are distinct in turn
from the 502 used for a genuine provider/network failure.

**`/v1/auth/linkedin/login` and `/v1/auth/linkedin/callback` are excluded
from the existing `ApiKeyAuthFilter`.** Caught by an actual local run,
not by inspection: both endpoints are reached by a browser, not an API
client — the user's own browser hits `/login`, and LinkedIn's server
then redirects that same browser to `/callback`. Neither can attach a
custom `X-API-Key` header, so gating them like `/v1/profile` would make
the flow permanently return 401 before ever reaching the controller.
CSRF protection for this flow comes from `OAuthStateStore`'s single-use
state parameter, not the API key.

**`GlobalExceptionHandler` gets one additional, purely additive handler
for `NoResourceFoundException`, mapping it to a clean 404 instead of the
existing generic 500.** This is a genuine gap in `add-profile-lookup`'s
already-shipped exception handler, but it was invisible until now:
every other `/v1/**` path is intercepted by `ApiKeyAuthFilter` before
Spring MVC ever attempts a handler lookup, so "no handler found" was
unreachable dead code. Excluding `/v1/auth/linkedin/**` from that filter
(the decision above) is what exposes it — requesting that path while
`profile.source` isn't `oidc` now genuinely reaches Spring MVC with no
matching handler. Caught by an actual local run (`mvn spring-boot:run`
in fixture mode, then a manual request to the login endpoint), not by
inspection. No existing handler method changes; this is a new method
alongside them.

**`profile.source=oidc` and its supporting beans (state store, OIDC
client, adapter, controller) are all wired behind
`@ConditionalOnProperty(profile.source=oidc)`**, matching
`ProfileSourceConfig`'s existing selection mechanism. This means the
`LINKEDIN_CLIENT_ID`/`LINKEDIN_CLIENT_SECRET`/`LINKEDIN_REDIRECT_URI`
validation (fail fast, at startup, with a clear `IllegalStateException`
if any are blank) only ever runs when `oidc` mode is actually selected —
running `fixture` or `stub` mode never requires LinkedIn credentials to
be present.

## Risks / Trade-offs

- [Single in-memory slot means only one "logged in" identity at a time,
  system-wide] → Explicitly a demo-scoped limitation, stated in the
  README next to this feature, not discovered by a confused second user.
- [State values live in memory with no persistence] → A restart mid-flow
  invalidates any pending login attempt. Acceptable; the user just
  starts over.
- [Requiring the caller to assert their own `profileUrl` is a
  self-reported value LinkedIn never confirms] → Acceptable for a demo
  whose whole point is "you sign in as yourself"; nothing about this
  adapter's honesty claims depends on that URL being independently
  verified, only on the name/photo actually coming from LinkedIn's own
  consent flow.
- [This still does not satisfy the challenge's core requirement] → Not a
  risk to mitigate, a fact to keep stating plainly. See proposal.md,
  "Why." Nothing about implementing this well changes what it is.

## Migration Plan

1. Register a LinkedIn Developer App and configure
   `LINKEDIN_CLIENT_ID`/`LINKEDIN_CLIENT_SECRET`/`LINKEDIN_REDIRECT_URI`
   in the target environment.
2. Deploy with `PROFILE_SOURCE` still `fixture` (or `stub`) until the
   above is confirmed; switch to `PROFILE_SOURCE=oidc` only once the
   LinkedIn app's redirect URL matches the deployed
   `LINKEDIN_REDIRECT_URI` exactly.
3. Roll back by returning to `PROFILE_SOURCE=fixture`; no persisted data
   or migration is introduced by this change, since the consented-profile
   slot is in-memory only.
