# Profile Lookup API

Hosted API that accepts a LinkedIn profile URL and returns structured JSON.
**Default data source is the live LinkedIn HTTP adapter** (`profile.source=linkedin`).
Optional `fixture` and `stub` sources remain available for offline demos and tests.

## Run locally

### Prerequisites

- **JDK 17 or newer** (JDK 21/23 also work). On Windows, set `JAVA_HOME` to that JDK for the shell you use — if it still points at an older install (for example Java 8), Maven may fail with `UnsupportedClassVersionError` even when `java -version` looks fine.
- No separate Maven install is required; the Maven Wrapper (`mvnw` / `mvnw.cmd`) is committed.

### 1. Clone and enter the project

```bash
git clone https://github.com/bhanu-1902/linkedin-profile-lookup-api.git
cd linkedin-profile-lookup-api
```

### 2. Configure environment

Default source is live LinkedIn (`PROFILE_SOURCE=linkedin`). Copy the example env file and fill in values — **never commit a real `.env`** (it is gitignored):

```bash
cp .env.example .env
```

Required for live mode:

| Variable | Description |
| --- | --- |
| `LINKEDIN_SESSION_COOKIE` | Full `Cookie` header value (for example `li_at=...; JSESSIONID="ajax:..."`) |
| `LINKEDIN_PROFILE_URL_TEMPLATE` | Upstream URL template containing `{handle}` |

Optional:

| Variable | Description |
| --- | --- |
| `LINKEDIN_CSRF_TOKEN` | CSRF token if the upstream expects it (often the `ajax:…` value) |
| `LINKEDIN_USER_AGENT` | Defaults to `Mozilla/5.0` |
| `LINKEDIN_TIMEOUT_MS` | Defaults to `8000` |
| `PORT` | Defaults to `8080` |

You can also export the same variables in the shell instead of using `.env`. Spring Boot reads process environment variables either way.

`LINKEDIN_PROFILE_URL_TEMPLATE` must return **JSON** the mapper can read (`name` or `firstName`/`lastName`, `headline`, and related fields). A normal HTML profile page such as `https://www.linkedin.com/in/{handle}` typically results in HTTP **502** (`source-upstream-error`).

### 3. Start the application

**Windows (PowerShell):**

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-23'   # adjust to your JDK 17+ path
$env:LINKEDIN_SESSION_COOKIE = 'li_at=...; JSESSIONID="ajax:..."'
$env:LINKEDIN_CSRF_TOKEN = 'ajax:...'             # optional
$env:LINKEDIN_PROFILE_URL_TEMPLATE = 'https://example.com/profiles/{handle}'
.\mvnw.cmd spring-boot:run
```

**macOS / Linux:**

```bash
export LINKEDIN_SESSION_COOKIE='li_at=...; JSESSIONID="ajax:..."'
export LINKEDIN_CSRF_TOKEN='ajax:...'             # optional
export LINKEDIN_PROFILE_URL_TEMPLATE='https://example.com/profiles/{handle}'
./mvnw spring-boot:run
```

Wait until the log shows Tomcat started on port `8080` (or your configured `PORT`).

### 4. Verify the service is up

```bash
curl http://localhost:8080/actuator/health
```

Expected: `{"status":"UP"}` (or equivalent healthy payload).

Useful URLs while the app is running:

| URL | Purpose |
| --- | --- |
| `http://localhost:8080/docs` | Swagger UI |
| `http://localhost:8080/v3/api-docs` | OpenAPI document |
| `http://localhost:8080/actuator/health` | Health check |

### 5. Test the profile endpoint

**Successful lookup** (replace the handle with a profile your upstream can resolve):

```bash
curl -i "http://localhost:8080/v1/profile?url=https://www.linkedin.com/in/<handle>"
```

**PowerShell equivalent:**

```powershell
Invoke-WebRequest -Uri "http://localhost:8080/v1/profile?url=https://www.linkedin.com/in/<handle>" -UseBasicParsing
```

**Validation error** (malformed URL → `400`):

```bash
curl -i "http://localhost:8080/v1/profile?url=not-a-linkedin-url"
```

**Live miss** (well-formed URL with no upstream profile → `404`):

```bash
curl -i "http://localhost:8080/v1/profile?url=https://www.linkedin.com/in/definitely-not-a-real-handle-xyz"
```

**Auth failure** (stop the app, unset the cookie, restart, then call again → `503` `source-unauthenticated`):

```bash
# omit LINKEDIN_SESSION_COOKIE, then:
curl -i "http://localhost:8080/v1/profile?url=https://www.linkedin.com/in/<handle>"
```

### Optional: offline sources (no LinkedIn credentials)

Use these when you only need to exercise the HTTP layer without a live upstream:

```bash
# Windows
$env:PROFILE_SOURCE = 'fixture'
.\mvnw.cmd spring-boot:run

# macOS / Linux
PROFILE_SOURCE=fixture ./mvnw spring-boot:run
```

Then:

```bash
curl "http://localhost:8080/v1/profile?url=https://www.linkedin.com/in/example-profile"
curl -i "http://localhost:8080/v1/profile?url=https://www.linkedin.com/in/not-in-fixtures"
```

Expected: `200` for the sample fixture profile; `501` for an unknown URL in fixture/stub mode.

`PROFILE_SOURCE=stub` always returns the non-live miss path (`501`).

### Run the automated tests

```bash
# Windows
.\mvnw.cmd test

# macOS / Linux
./mvnw test
```

Integration tests pin `profile.source=fixture`, so the suite does not require LinkedIn credentials.
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
