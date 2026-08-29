## 1. Default to live LinkedIn

- [x] 1.1 Set `profile.source` default to `linkedin` in `application.yml`
- [x] 1.2 Move `matchIfMissing` to the LinkedIn bean in `ProfileSourceConfig`
- [x] 1.3 Align `ProfileController` `@Value` default with `linkedin`
- [x] 1.4 Update `.env.example` and `render.yaml` for live-first setup

## 2. Tests

- [x] 2.1 Wiring test expects LinkedIn when property unset; fixture/stub when set
- [x] 2.2 Integration / OpenAPI / rate-limit / source-failure tests pin `fixture`
- [x] 2.3 Keep LinkedIn unit tests on FakeGateway (no real cookie)

## 3. Docs

- [x] 3.1 Rewrite README for live quickstart; fixture/stub as optional
- [x] 3.2 Update `openspec/specs/profile-lookup/spec.md` miss scenarios
- [x] 3.3 Fill this change's proposal/design/tasks
