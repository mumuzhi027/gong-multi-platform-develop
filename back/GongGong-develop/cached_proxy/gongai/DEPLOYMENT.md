# Gong AI Beta deployment gate

The code intentionally fails closed for provider writes that cannot be verified.
Do not replace these checks with mock success responses in production.

## Required configuration

- Set `DEEPSEEK_API_KEY` through the deployment secret store.
- Keep `DEEPSEEK_MODEL=deepseek-v4.1-flash` unless the provider publishes a different exact identifier.
- Set `SESSION_VAULT_MASTER_KEY` to a randomly generated deployment secret with at least 32 bytes of entropy.
- Provision DNS and a valid TLS certificate for the app API/update hosts, then switch `SystemGlobalConfig` and Android network security back to HTTPS-only. The current compatibility default still uses the existing HTTP IP because the proposed HTTPS host has no DNS record; this does not satisfy the production security gate.
- Remove any historical proxy volume containing the old account Gob repository before deployment. Current code no longer writes campus passwords.

## External integration TODOs

The repository does not contain a documented, current-user-bound SSO exchange from the information portal to `sc.xtu.edu.cn`. It also does not contain a captured current deployment form/schema response for the room reservation page. Consequently:

- `leave.status`, `leave.apply`, and `leave.withdraw` fail closed until the extracurricular ProviderSession bootstrap is supplied.
- attachment upload UI/API remains disabled until that owner-bound provider session can upload directly to `/sc-api/infra/file/upload` and store only an encrypted reference;
- non-empty-room `room.search` and `room.reserve` fail closed until the actual Xiangtan University page fields are validated.

After supplying those integrations, wire them through `CampusTools`. A write adapter must query the active schema/definition at runtime, execute at most once, re-query the upstream record, and return `OK` only after verification.

## Release checks

Run:

```text
go test ./...
go vet ./...
python -m compileall -q ems-sdk
gradlew.bat :composeApp:compileDebugKotlinAndroid
gradlew.bat :composeApp:testDebugUnitTest
```

The committed integration test no longer contains a campus credential and is disabled by default. Run live integration tests only from an external harness with secrets injected at runtime.
