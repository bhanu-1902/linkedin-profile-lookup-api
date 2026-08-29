## Context

`ProfileSource` already isolates retrieval. The LinkedIn HTTP adapter
(`LinkedInHttpProfileSource` + `RestLinkedInGateway` + `LinkedInProfileMapper`)
is the default runtime implementation.

## Goals / Non-Goals

**Goals:**
- Default runtime source is `linkedin`.
- Classified upstream failures (401/403/429/5xx/unusable body) map to stable problem+json.
- Live miss → 404; optional fixture/stub miss → 501.
- CI stays offline by pinning tests to `fixture` where needed.

**Non-Goals:**
- Removing fixture/stub packages.
- HTML scraping or browser automation.
- Committing session cookies.

## Decisions

**Factory via `profile.source`.** `linkedin` has `matchIfMissing = true`.
`fixture` and `stub` require an explicit property.

**Fail closed on auth.** Missing session cookie → `UNAUTHENTICATED` → 503.
Invalid/expired session from upstream 401/403 → same.

**JSON mapper only.** Upstream body must be JSON. HTML or empty usable
payload → empty Optional or `UPSTREAM_ERROR` (502).

**Secrets in env.** Cookie, optional CSRF, and URL template come from
environment / host secret store — never from git-tracked files with real values.
