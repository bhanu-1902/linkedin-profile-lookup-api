# Profile Lookup API

Hosted API that accepts a LinkedIn profile URL and returns structured JSON.
**Default data source is the live LinkedIn HTTP adapter** (`profile.source=linkedin`).
Optional `fixture` and `stub` sources remain available for offline demos and tests.

## Quickstart

Requires JDK 17+. Maven Wrapper is committed.

```bash
git clone <this-repo>
cd profile-lookup-api
```

### Live LinkedIn (default)

Set credentials in the environment (never commit them). Copy `.env.example` to `.env` if you prefer a local file:

```powershell
# PowerShell
$env:LINKEDIN_SESSION_COOKIE = '<cookie header value>'
$env:LINKEDIN_CSRF_TOKEN = '<optional>'
$env:LINKEDIN_PROFILE_URL_TEMPLATE = '<JSON URL template containing {handle}>'
.\mvnw.cmd spring-boot:run
```

```bash
# macOS / Linux
export LINKEDIN_SESSION_COOKIE='...'
export LINKEDIN_CSRF_TOKEN='...'   # optional
export LINKEDIN_PROFILE_URL_TEMPLATE='...{handle}...'
./mvnw spring-boot:run
```

`LINKEDIN_PROFILE_URL_TEMPLATE` must return **JSON** the mapper can read
(`name` or `firstName`/`lastName`, `headline`, etc.). A normal HTML
`/in/{handle}` page will typically surface as HTTP **502**
(`source-upstream-error`).

```bash
curl "http://localhost:8080/v1/profile?url=https://www.linkedin.com/in/<handle>"
```

Interactive docs: `http://localhost:8080/docs`  
OpenAPI: `http://localhost:8080/v3/api-docs`  
Health: `http://localhost:8080/actuator/health`

### Optional offline sources

```bash
# Bundled sample JSON (no LinkedIn call)
PROFILE_SOURCE=fixture ./mvnw spring-boot:run

# Always empty (honest miss path for non-live modes)
PROFILE_SOURCE=stub ./mvnw spring-boot:run
```

**Windows note:** if `mvnw.cmd test` fails with `UnsupportedClassVersionError ... class file version 61.0`, point `JAVA_HOME` at JDK 17+ for that shell.

Run tests: `./mvnw test` or `mvnw.cmd test` (integration tests pin `fixture` so CI needs no cookie).

## API

`GET /v1/profile?url=<LinkedIn profile URL>`

| Status | Meaning |
| --- | --- |
| 200 | Profile found; absent fields omitted |
| 400 | Missing or invalid LinkedIn profile URL |
| 404 | Live source configured; no profile for this URL |
| 429 | Caller rate limit or upstream rate limit (`Retry-After`) |
| 501 | Non-live source (`fixture`/`stub`) and URL not available |
| 502 | Configured source returned an unusable response |
| 503 | Source unavailable or could not authenticate |

## Architecture

Clean Architecture / ports-and-adapters with one port:

```
domain/           Profile, ProfileSource, LinkedInProfileUrls, ProfileSourceException
application/      ProfileLookupService
infrastructure/   LinkedInHttpProfileSource (default), FixtureProfileSource, StubProfileSource
interfaces/rest/  Controller, DTOs, rate limit filter, exception handling
```

`ProfileSourceConfig` is the only place that knows which adapters exist.
Selection: `profile.source=linkedin|fixture|stub` (default `linkedin`).

## Public access and rate limiting

No API key. `/v1/**` is rate-limited per caller address (Bucket4j token
bucket; `RATELIMIT_CAPACITY` / `RATELIMIT_REFILL_SECONDS`). Exceeding the
limit returns 429 + `Retry-After` + problem+json. Buckets are in-memory
(single-instance demo scale).

## Source failures

Live adapter failures map to stable problem+json types:

| Failure | HTTP | `type` suffix | `Retry-After` |
| --- | --- | --- | --- |
| Cannot authenticate | 503 | `source-unauthenticated` | — |
| Temporarily unavailable | 503 | `source-unavailable` | — |
| Provider rate-limited | 429 | `source-rate-limited` | when known |
| Unusable upstream body | 502 | `source-upstream-error` | — |

## Secrets

Do not commit session cookies or CSRF tokens. Use environment variables
or your host’s secret store (see `.env.example` and `render.yaml`).

## Deploying

```bash
docker build -t profile-lookup-api .
docker run -p 8080:8080 \
  -e PROFILE_SOURCE=linkedin \
  -e LINKEDIN_SESSION_COOKIE=... \
  -e LINKEDIN_PROFILE_URL_TEMPLATE=... \
  profile-lookup-api
```

`render.yaml` targets Render’s free tier with `PROFILE_SOURCE=linkedin`.
Add `LINKEDIN_*` values in the Render dashboard.

CI (`.github/workflows/ci.yml`) runs tests, packages the jar, builds the
image, and validates OpenSpec on push/PR.

## Spec-driven development

`openspec/specs/profile-lookup/spec.md` is the source of truth for the
endpoint contract. Active change work for the live adapter lives under
`openspec/changes/add-linkedin-http-source/`. Archived fixture-first
history remains under `openspec/changes/archive/` for context only.
