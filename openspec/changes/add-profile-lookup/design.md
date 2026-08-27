## Context

See proposal.md - Why. Constraint that shapes everything below: no
sanctioned LinkedIn API returns a third party's profile by URL, and the
only implementation that would satisfy the brief literally is a
credential-based scraper, which is out of scope by decision (see
"Rejected approach" under Decisions). Target runtime: Java 17+, Spring
Boot 4.1.x (Spring Framework 7 / Jakarta EE 11).

## Goals / Non-Goals

**Goals:**
- Isolate profile retrieval behind one substitutable boundary so the rest
  of the system is provably indifferent to where profile data comes from.
- Ship a system a reviewer can run locally in one command and exercise
  end-to-end against real (fixture) data.
- Keep the architecture right-sized for a handful of endpoints — no
  layering introduced without a concrete need for it.

**Non-Goals:**
- Fetching data from LinkedIn at runtime, by any method.
- Supporting profile fields beyond what a legitimate LinkedIn data export
  contains.
- Multi-tenant API key management (a small fixed set of keys is enough
  for a demo).

## Decisions

**Ports-and-adapters (Clean Architecture), one port.** `ProfileSource` is
the only interface with more than one implementation. Alternatives
considered: a full four-layer/four-module split (rejected — no second
implementation exists for anything else, so extra interfaces would be
ceremony, not architecture); no abstraction at all, data source called
directly from the controller (rejected — makes the "documented stub, real
adapter later" story impossible to demonstrate cleanly).

**Adapter selection via Spring `@ConditionalOnProperty` (Factory +
Strategy).** `profile.source=fixture|stub` selects the bean. Alternative
considered: a hand-rolled factory class switching on an enum — rejected as
strictly more code for the same behavior once Spring's conditional beans
do it declaratively.

**No Spring Security.** API-key checking is a single `OncePerRequestFilter`
comparing a header against a configured set of keys. Alternative
considered: Spring Security with a custom `AuthenticationFilter` —
rejected for two reasons: (1) Spring Security 7's default CSRF posture
changed in ways that are easy to misconfigure for a stateless,
non-session API and hard to verify without a live build; (2) pulling in
the full framework for one header comparison is disproportionate to the
problem. This is the kind of judgment call worth stating plainly in the
README rather than hiding.

**Bucket4j core directly, not a Spring-integration starter.** `bucket4j-core`
has no Spring dependency, so its Spring Boot 4 compatibility isn't in
question. Buckets are held in a `ConcurrentHashMap` keyed by API key —
sufficient for a single instance; documented as a scaling limitation
below rather than solved with Redis pre-emptively.

**RFC 9457 via Spring's native `ProblemDetail`.** Available since Spring
Framework 6; no extra dependency. One `@RestControllerAdvice` produces
every error response, including the "no live data source" case, as a
typed problem+json body rather than an undifferentiated 404 or 500.

**Fixtures derived from a real LinkedIn data export, not invented data.**
The shape must match what LinkedIn actually exports (Profile.csv,
Positions.csv, Education.csv, Skills.csv, Certifications.csv,
Languages.csv), so the fixture loader is honest about the data's real
shape rather than a schema invented for convenience.

**Rejected approach: a third-party "people data" broker adapter (e.g.
People Data Labs).** Considered during a post-implementation review that
otherwise caught real bugs (see Risks/Trade-offs addendum below) and
explicitly rejected. On its face this looks different from scraping
LinkedIn directly -- it's a paid API, not credentials in the backend.
It isn't different in the way that matters. PDL's own documentation
describes two data-source categories, one of which is "public data
sources" (the public web); an independent 2025 industry analysis names
PDL specifically as sourcing via "web scraping (including public
LinkedIn profiles)"; PDL was the identified source of 420M+ LinkedIn
URLs in the 2019 exposure of 1.2 billion records; and PDL agreed to a
$6.36M settlement (preliminarily approved June 30, 2026) over including
people's phone numbers in a commercial directory without consent. Using
a broker changes which party is the proximate one doing non-consensual
collection. It does not change what the deployed system does: given any
LinkedIn URL, return that person's professional history, obtained
without their consent, served through a public API. The one broadly
sanctioned "live" path remains what's noted below: OAuth for the
*authenticated caller's own* profile, which doesn't satisfy the
brief's literal ask (arbitrary third-party lookup) and is exactly why
it's out of scope rather than built.

**Rejected approach: a `LinkedInScrapeProfileSource` adapter.** Considered
and explicitly rejected. It would satisfy the brief's literal wording but
requires authenticating to LinkedIn with real credentials and retrieving
third parties' data by automated means, which LinkedIn's User Agreement
prohibits (§8.2) and has enforced through litigation (hiQ Labs v.
LinkedIn; LinkedIn Corp. v. Nubela/Proxycurl, 2025). Because
`ProfileSource` is a port, adding this adapter later would require zero
changes to the domain, application layer, or controller — the boundary is
exactly where such an adapter would plug in, which is itself the point:
the architecture doesn't foreclose it, it isolates it, and it isn't built.

## Risks / Trade-offs

- [Single-instance rate limiting] → Documented in README as a known
  scaling limit; migration path is Bucket4j's distributed mode with a
  Redis backend, not implemented here.
- [Fixture set is necessarily small] → Acceptable for a demo; the
  contract test suite is what actually proves the port abstraction holds,
  not fixture coverage breadth.
- [Spring Boot 4.1.x moved fast in 2026 and some dependency coordinates
  may drift] → `pom.xml` pins versions verified at write time; first
  `mvn clean install` is the real compatibility check, called out in the
  README as an expected first step rather than assumed already clean.

- [Sandbox that wrote this code cannot reach Maven Central] → A real
  local `mvn`/`docker` build is the actual first verification pass, not
  something already confirmed here. A first such attempt did catch a
  real bug (`--` inside an XML comment in `pom.xml` -- illegal
  anywhere in a comment's body, not just at the boundaries) that only a
  real `mvn`/IDE parse could have caught. Treat every claim in this
  document about Spring-dependent code as "written carefully," not
  "verified," until your own build says otherwise -- the domain layer
  (compiled standalone with plain `javac`, repeatedly, including after
  later additions) is the one part of this codebase where "verified" is
  actually earned.

## Migration Plan

N/A — this is the first capability in a new project; there is no prior
version to migrate from or roll back to.
