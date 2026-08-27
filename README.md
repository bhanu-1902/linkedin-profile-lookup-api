# Profile Lookup API

A hosted API that accepts a profile URL and returns structured JSON —
built to the shape of a take-home brief that, read literally, asks for a
credential-based LinkedIn scraper. This is the honest version instead:
every skill the brief evaluates (architecture, API design, testing,
deployment, documentation, judgment) is demonstrated, without shipping
something that violates LinkedIn's Terms of Service or scrapes real
people's data without consent.

**If you only read one section, read "Known limitations & legal
considerations" below** — it's not an apology, it's the point.

## Quickstart

```bash
git clone <this-repo>
cd profile-lookup-api
mvn spring-boot:run
```

No Maven installed? Open the folder in IntelliJ IDEA or VS Code with the
Java extension — both auto-detect `pom.xml` and can run
`ProfileLookupApiApplication` directly with no separate Maven install.

Once running (default port 8080):

```bash
# A profile that exists in the bundled fixture:
curl -H "X-API-Key: demo-key-change-me" \
  "http://localhost:8080/v1/profile?url=https://www.linkedin.com/in/example-profile"

# A well-formed URL with no fixture data -- the honest-failure path:
curl -H "X-API-Key: demo-key-change-me" \
  "http://localhost:8080/v1/profile?url=https://www.linkedin.com/in/anyone-else"

# No API key:
curl "http://localhost:8080/v1/profile?url=https://www.linkedin.com/in/example-profile"
```

Interactive docs: `http://localhost:8080/docs` (Swagger UI, via
springdoc). OpenAPI document: `http://localhost:8080/v3/api-docs`.
Health check: `http://localhost:8080/actuator/health`.

Run the tests: `mvn test`.

## The problem, honestly stated

The brief: accept a LinkedIn profile URL, return name, headline,
location, about, experience, education, skills, certifications,
languages, and images as JSON, hosted publicly over HTTPS.

**LinkedIn provides no sanctioned API for this.** Its only self-serve
product, Sign In with LinkedIn (OpenID Connect), returns data solely for
the *authenticated caller* via `GET /v2/userinfo` — eight fields (name,
picture, locale, email), and only for the person who just logged in.
There is no parameter to request a third party's profile by URL. Every
richer LinkedIn API (Sales Navigator, Talent Solutions, Marketing
Developer Platform) requires a formal partnership LinkedIn approves
case-by-case, with reported wait times of weeks to months — not
realistic for a take-home, and not something a general developer account
can get.

That means fulfilling the brief *literally* requires one thing: an
automated tool that logs into LinkedIn with real credentials and scrapes
arbitrary profiles. This project deliberately does not build that. See
below for why, and see
`openspec/changes/add-profile-lookup/design.md` ("Rejected approach")
for where that decision is formally recorded.

## Architecture

Clean Architecture / ports-and-adapters, sized to actually fit a project
this small — one port, not a sprawling hexagon:

```
domain/           Profile entity, ProfileSource port. Zero framework
                   imports -- compiles with plain javac, nothing else.
application/       ProfileLookupService (the use case). Depends only on
                   the ProfileSource interface.
infrastructure/    FixtureProfileSource, StubProfileSource -- the two
                   adapters actually shipped -- plus the config that
                   selects between them.
interfaces/rest/   Controller, DTOs, filters, exception handling.
                   Depends on the use case; knows nothing about fixtures
                   or stubs.
```

Dependencies point inward only. `ProfileLookupService` has never heard of
`FixtureProfileSource`. `ProfileController` has never heard of either
adapter. The only file that knows both adapters exist is
`ProfileSourceConfig`.

### GoF patterns, used because they fit — not for their own sake

- **Builder** (`Profile.Builder`) — the domain entity has many optional
  fields; this avoids a telescoping constructor. The response DTO
  deliberately does *not* get a second builder — a straight 1:1 field
  copy doesn't need one, and a redundant builder there would be exactly
  the "presenter pass-through" over-engineering worth avoiding.
- **Strategy** (`ProfileSource` implementations) — the fixture and stub
  adapters are interchangeable strategies behind one interface.
- **Factory** (`ProfileSourceConfig`) — selects which strategy gets wired
  in, via a config property, using Spring's `@ConditionalOnProperty`
  rather than a hand-rolled switch.
- **Adapter**, in the classic GoF sense — reserved for exactly the seam
  where a real third-party integration (LinkedIn OAuth for the
  *authenticated user's own* profile, say) would plug in later, without
  touching anything upstream of `ProfileSource`.

## Decisions & trade-offs

**No Spring Security for API-key auth.** A single header comparison
doesn't need a security framework. Spring Security 7 (the version
shipping with Spring Boot 4) changed CSRF defaults in ways that
silently 403 REST APIs if misconfigured, and that's a hard thing to get
right without a live build to test against — which this project didn't
have (see "How this was built," below). A plain `OncePerRequestFilter`
is simpler, has less configuration surface, and does exactly the one
thing needed.

**Bucket4j core directly, not a Spring-integration starter.**
`bucket4j-core` has zero Spring dependency, so its compatibility with
whatever Spring Boot version you're on isn't a question. Buckets are
kept in a `ConcurrentHashMap`, one per API key — correct for a single
instance, and documented as a scaling limit rather than solved
pre-emptively with Redis nobody asked for yet.

**RFC 9457 via Spring's native `ProblemDetail`**, not a hand-rolled error
DTO. Available since Spring Framework 6, so it costs nothing extra, and
every error response — validation failure, missing API key, rate limit,
unavailable profile — uses the same shape with a stable `type` URI a
client can match on.

**Fixtures are derived from a real LinkedIn export's actual shape**
(Profile / Positions / Education / Skills / Certifications / Languages),
not an invented schema. The bundled sample is placeholder data — replace
`src/main/resources/fixtures/sample-profiles.json` with data from your
own export (Settings & Privacy → Data Privacy → "Get a copy of your
data") to demo against something real.

**OpenSpec from the first commit**, not retrofitted. `openspec/changes/
add-profile-lookup/` contains the actual proposal → specs → design →
tasks that were written and validated (`openspec validate --changes`)
before any code — the Gherkin-style scenarios in `specs/profile-lookup/
spec.md` are exactly what `ProfileControllerIntegrationTest` tests
against, one scenario per test method, deliberately.

## Known limitations & legal considerations

This is scoping, not an excuse.

**No live LinkedIn adapter is shipped, and here's the actual legal
landscape that decision rests on:**

- **LinkedIn's User Agreement (§8.2, "Don'ts")** prohibits scraping,
  bots or automated access, and using another's account or copied
  session credentials — in the same clause.
- **hiQ Labs v. LinkedIn** — widely mis-cited as "scraping LinkedIn is
  legal." The Ninth Circuit did hold that scraping *purely public* pages
  likely doesn't violate the CFAA. But the case ended with LinkedIn
  **winning on breach of contract**: a **$500,000 judgment against hiQ**
  (Dec 2022 consent judgment), hiQ ordered to destroy scraped data and
  code, and hiQ is now defunct. The part that survives as a warning:
  logging into an account governed by a User Agreement, then scraping,
  is a contract claim CFAA rulings don't touch.
- **LinkedIn Corp. v. Nubela Pte. Ltd. (Proxycurl)**, filed January 2025
  — the direct precedent for *this exact product shape*. Proxycurl was a
  hosted API turning LinkedIn profile URLs into structured JSON,
  allegedly via automated/fake-account access. LinkedIn sued (breach of
  contract, CFAA, fraud, Lanham Act); Proxycurl shut down in mid-2025 as
  part of the resulting settlement, after reportedly reaching ~$10M in
  annual revenue first.
- **Meta Platforms v. Bright Data** (N.D. Cal., Jan 2024) is the
  strongest case *for* scraping — but it explicitly turned on
  **logged-out** access to public pages, the opposite of what a
  credential-based LinkedIn integration would require. It reinforces
  the same line: authenticated access is where the contract theory
  bites; logged-out public access is comparatively safer ground.

**The pattern across all four: credential-based / fake-account scraping
is where enforcement concentrates.** A dummy account changes whose
account is on the line, not whether the activity violates the
agreement — Proxycurl's core allegation was exactly "created fake
accounts to scrape," and that's what turned it into a CFAA and fraud
case, not just a contract dispute.

**What this means for extending this project:** the `ProfileSource`
interface is exactly where a real adapter would go, and the architecture
doesn't prevent one from being added — but nothing here does, and if you
add one, that's a decision to make with full awareness of the above, not
something this codebase does for you.

**Other honest limitations:**
- Rate limiting is per-instance (in-memory buckets); running >1 instance
  needs a shared backend (Bucket4j supports a distributed/Redis mode —
  not implemented here).
- The fixture set is intentionally small; the contract test is what
  actually proves the port abstraction holds, not fixture breadth.
- `demo-key-change-me` ships as the default API key so the quickstart
  works out of the box. **Set a real `PROFILE_API_KEYS` value for any
  non-local deployment** — this is flagged deliberately, not an
  oversight.

## How this was built — and what to check on your first `mvn` run

This project targets **Spring Boot 4.1.x** (current as of August 2026;
Boot moved to a new major version, 4.0, in November 2025 — if you're
used to 3.x, some defaults changed, notably Jackson 3 as the default
JSON library and stricter Spring Security 7 defaults, which is *why*
Spring Security was avoided here entirely). The dependency versions in
`pom.xml` were verified via web search at write time, not compiled — the
sandbox this was built in can reach npm and PyPI but not Maven Central.
The domain layer (`domain/*.java`) *was* compile-verified with plain
`javac`, since it has no framework dependencies to resolve. Everything
Spring-dependent was written carefully but is untested by a real build.

**Your `mvn clean install` is the actual first compile-and-fix cycle** —
treat it the way you would any fresh clone, not as something already
guaranteed green. The specific things most likely to need a small fix:

1. **`com.bucket4j:bucket4j-core` version / `io.github.bucket4j` package
   name** — verify both against Maven Central; the groupId moved at some
   point in the library's history and this wasn't independently
   confirmed for the current release.
2. **`springdoc-openapi-starter-webmvc-ui` version `3.0.3`** — this line
   moves fast for Boot 4 support; check for a newer 3.x patch.
3. **Spring Boot parent version `4.1.1`** — confirm it's still current,
   or bump the patch.

None of these affect the architecture if they need adjusting — they're
dependency-coordinate details, not design decisions.

## What I'd do with more time

- A real OAuth adapter behind `ProfileSource` for "Sign In with
  LinkedIn," returning the *authenticated user's own* profile — the one
  case LinkedIn does sanction, currently unimplemented because the
  fixture adapter covers the demo need.
- Distributed rate limiting (Bucket4j + Redis) for multi-instance
  deployment.
- A CI workflow running `openspec validate --strict`, `mvn test`, and a
  container build on every push.
- Structured JSON logging with a request-correlation ID threaded into
  the `ProblemDetail.instance` field.

## Deploying

`render.yaml` targets Render's free tier (auto HTTPS, git-push deploy;
free-tier services spin down after 15 minutes idle, ~30–60s cold start
on the next request — fine for a demo, not for a real SLA). Any
container host works identically via the `Dockerfile`:

```bash
docker build -t profile-lookup-api .
docker run -p 8080:8080 -e PROFILE_API_KEYS=your-real-key profile-lookup-api
```

## Spec-driven development with OpenSpec

`openspec/changes/add-profile-lookup/` holds the actual planning
artifacts written before implementation:

- `proposal.md` — why, and what's explicitly out of scope
- `specs/profile-lookup/spec.md` — SHALL/MUST requirements with
  WHEN/THEN scenarios (the acceptance criteria the integration test is
  written against)
- `design.md` — the architectural decisions above, plus the rejected
  scraper approach, in full
- `tasks.md` — the implementation checklist this was built from

Once everything in `tasks.md` is checked off end-to-end against a real
build, `openspec archive add-profile-lookup` moves the capability into
`openspec/specs/` as the project's source of truth for the next change.
