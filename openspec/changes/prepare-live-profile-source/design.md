## Context

See proposal.md for motivation and
specs/challenge-submission-readiness/spec.md for the observable contract. The
current code already has the useful core seam: the REST controller depends on
`ProfileLookupService`, which depends on the `ProfileSource` port. It also has
fixture and stub adapters, but the build does not compile records, the public
deployment is guarded by a generated key, and source failures cannot carry
safe, stable semantics to the HTTP boundary.

The future live source is intentionally unknown. This change must not encode
an endpoint, request format, credential type, or provider-specific response
schema before Tross replies.

## Goals / Non-Goals

**Goals:**

- Make the existing Java 17 source, automated tests, wrapper, container, and
  CI verifiably buildable.
- Preserve one small source port so the future work remains a single adapter.
- Define source outcomes that let an adapter distinguish absent data from a
  retryable or configuration failure without exposing provider internals.
- Let a challenge evaluator call the fixture deployment directly, with a
  simple per-client rate limit.
- Generate documentation from the actual HTTP contract and cover it with
  integration tests.

**Non-Goals:**

- Implementing a LinkedIn adapter, probing endpoints, accepting credentials,
  creating browser automation, or adding a third-party data provider.
- Adding a new module, repository, database, cache, queue, or authentication
  framework.
- Claiming a public GitHub repository or hosted deployment exists before the
  user creates those external resources.

## Decisions

### Pin the compiler release explicitly

Configure the Maven compiler plugin with an explicit Java 17 `release` value
rather than relying on inherited property translation. The project uses Java
records, so a source/target fallback to Java 8 is invalid. Keep JDK 21 in the
Docker build and CI because it can compile Java 17 bytecode; do not raise the
application language level merely to match the container image.

Alternative considered: replace records with Java 8 classes. Rejected because
it masks a broken build configuration and adds boilerplate to a Java 17
project.

### Keep `ProfileSource` as the sole extension point

Retain `Optional<Profile>` as the no-profile signal. Extend the existing
source exception with a small provider-failure classification and optional
retry interval. The controller maps that classification to stable API problem
types and safe details; it never forwards an upstream body, endpoint, or
credential information. A future adapter only needs to translate its direct
HTTP outcomes into this existing port contract.

Alternative considered: introduce a second source-result hierarchy or a
provider SDK abstraction now. Rejected because there is one future integration
and no approved endpoint shape yet; a richer abstraction would be ceremony.

### Remove the challenge API-key gate; retain rate limiting by client address

The submission endpoint must be usable by an evaluator without a secret that
Render generated and did not publish. Remove the custom API-key filter and its
configuration. Rate-limit `/v1/**` by the request's resolved client address,
continue to return RFC 9457 problem responses, and document the
single-instance limitation.

Alternative considered: publish a shared demonstration API key. Rejected
because it adds a needless evaluation step and makes a purported secret public.

### Treat OpenAPI as part of the tested contract

Annotate the profile endpoint and response/error types so the generated OpenAPI
document includes its query parameter, successful response, validation,
rate-limit, source-unavailable, and source-failure responses. Integration
tests assert the actual endpoint behavior; a focused documentation test checks
the generated document exposes the relevant response codes and retry header.

Alternative considered: maintain a separate handwritten OpenAPI file. Rejected
because it can drift from controller behavior.

### Preserve fixture-only deployment until approval

Keep `PROFILE_SOURCE=fixture` in the Render blueprint. The fixture adapter is
the only runtime provider and remains valuable as a deterministic test/demonstration
source after a real adapter is later introduced. README language will describe
this truthfully and state the live provider as the remaining dependency on
Tross's response.

## Risks / Trade-offs

- [Provider contract is designed before a provider exists] → Keep it to failure
  semantics only; endpoint-specific request and parsing logic remain deferred.
- [Rate limit is per instance and client address can be proxy-dependent] → Keep
  the implementation documented as challenge-scale; configure trusted proxy
  handling only when a concrete deployment requires it.
- [The existing unarchived change overlaps the same endpoint] → Resolved:
  `add-profile-lookup`'s delta spec was edited to match this change's
  final behavior (no API-key requirement, rate limiting by caller
  address) before archiving, so `openspec/specs/profile-lookup/spec.md`
  and this change's `challenge-submission-readiness` spec are
  non-contradictory. `add-oidc-self-lookup` was deleted outright (not
  archived) when the OIDC self-lookup adapter was removed from the
  codebase, rather than reconciled -- it described a feature no longer
  present.
- [Spring Boot and springdoc compatibility can drift] → Verify the full wrapper
  test and container build locally and in CI before any deployment.

## Migration Plan

1. Apply the build and API-access changes on the fixture deployment.
2. Run the wrapper tests, package the container, and verify the local health,
   OpenAPI, and fixture profile endpoints.
3. Deploy the fixture configuration and record its HTTPS URL in the README only
   after deployment succeeds.
4. If the Tross response approves a direct live source, create a follow-up
   OpenSpec change for the adapter. Configure its secrets only in the host's
   secret store, change the provider mode, and retain fixture tests as a
   regression baseline.
5. Roll back by returning to `PROFILE_SOURCE=fixture`; no persisted data or
   migration is introduced by this change.

## Open Questions

- Which endpoint family, authentication material, access scope, and operational
  constraints Tross expects for the later direct source. This does not block
  this preparatory change because no provider-specific code is introduced.
