## 1. LinkedIn app setup (you, not code)

- [ ] 1.1 Register a LinkedIn Developer App and confirm the "Sign In with
  LinkedIn using OpenID Connect" product is enabled (self-serve — unlike
  the partner-gated products, this one is confirmed open to individual
  developers)
- [ ] 1.2 Note the Client ID, Client Secret, and set the app's authorized
  redirect URL to match `LINKEDIN_REDIRECT_URI`

## 2. State management

- [x] 2.1 Implement an in-memory state store (issue, verify-and-consume,
  expire) and verify with a unit test: issue then consume once succeeds,
  consuming the same state twice fails, an unissued state fails
- [x] 2.2 Verify expired states are rejected even if otherwise well-formed

## 3. The OIDC adapter

- [x] 3.1 Implement `LinkedInOidcProfileSource implements ProfileSource`
  with an explicit method to record a newly-consented profile (called by
  the callback handler), and verify it satisfies
  `ProfileSourceContractTest` like the other two adapters
- [x] 3.2 Verify a lookup for the exact consented URL returns real data,
  and a lookup for any other URL returns empty — both via a direct unit
  test, not only through the full HTTP flow

## 4. HTTP endpoints

- [x] 4.1 Implement `GET /v1/auth/linkedin/login`: issue a state, build
  the LinkedIn authorization URL, redirect. Verify the redirect target's
  query parameters are correct.
- [x] 4.2 Implement `GET /v1/auth/linkedin/callback`: validate state,
  exchange code for token, call userinfo, record the profile, redirect
  or respond with a simple confirmation. Verify with a mocked
  `RestClient` for both the token and userinfo calls.
- [x] 4.3 Verify the consent-declined path (LinkedIn's `error` query
  parameter) produces the typed "declined" response, not a generic
  failure
- [x] 4.4 Verify a token-exchange or userinfo failure (mocked
  non-2xx/malformed response) produces a 502 problem+json, and that no
  profile gets recorded from a partial result

## 5. Wiring

- [x] 5.1 Add the `profile.source=oidc` branch to `ProfileSourceConfig`
  and verify the application starts successfully with each of the three
  `profile.source` values
- [x] 5.2 Add `LINKEDIN_CLIENT_ID` / `LINKEDIN_CLIENT_SECRET` /
  `LINKEDIN_REDIRECT_URI` to `application.yml` and `.env.example`, and
  verify the app fails fast with a clear error if `oidc` is selected but
  these are unset — not a silent NullPointerException three calls later

## 6. Documentation

- [x] 6.1 Update the README: how to register a LinkedIn app, how to run
  the flow locally, and an explicit restatement next to this feature
  that it only ever serves the one consented profile — not a general
  lookup path
- [x] 6.2 Verify the OpenAPI document describes both new endpoints,
  including the error responses from tasks 4.3 and 4.4

## 7. Archive

- [ ] 7.1 Run `openspec archive add-oidc-self-lookup` once every item
  above has an actually-met verification, the same standard applied to
  `add-profile-lookup`
