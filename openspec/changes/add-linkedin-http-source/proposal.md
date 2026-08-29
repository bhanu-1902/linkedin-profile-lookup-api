## Why

The API should look up profiles through the live LinkedIn HTTP adapter by
default. Fixture and stub remain optional for offline demos and CI.

## What Changes

- Default `profile.source` is `linkedin` (`matchIfMissing` on that bean).
- Live misses return HTTP 404; fixture/stub misses still return 501.
- README, `.env.example`, and Render blueprint describe live-first setup.
- Integration tests that must run without credentials pin `profile.source=fixture`.

## Capabilities

### New Capabilities

- `linkedin-http-source`: session-cookie HTTP adapter behind `ProfileSource`,
  selected by default at runtime.

### Modified Capabilities

- `profile-lookup`: live miss → 404; non-live miss → 501; default source is live.

## Impact

- Runtime requires `LINKEDIN_SESSION_COOKIE` and a JSON-returning
  `LINKEDIN_PROFILE_URL_TEMPLATE` for successful live lookups.
- Offline `PROFILE_SOURCE=fixture|stub` still works.
