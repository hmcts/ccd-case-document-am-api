# JWT issuer validation

## Service

`ccd-case-document-am-api`

## Summary

- JWT issuer validation is enabled in the active `JwtDecoder`.
- OIDC discovery and issuer enforcement are configured separately on purpose.
- The enforced issuer must be taken from a real access token `iss` claim, not inferred from discovery metadata or deployment naming.

## Discovery vs enforced issuer

- `spring.security.oauth2.client.provider.oidc.issuer-uri` is the discovery location. The service uses it to load OIDC metadata and the JWKS endpoint.
- `oidc.issuer` / `OIDC_ISSUER` is the enforced issuer value. The active `JwtDecoder` validates the token `iss` claim against this value.
- These values can differ. Discovery can point at the public IDAM OIDC endpoint while enforcement pins the exact `iss` emitted in real access tokens.

## Runtime behavior

- `SecurityConfiguration.jwtDecoder()` builds the decoder from `issuer-uri`.
- The decoder then applies both `JwtTimestampValidator` and `JwtIssuerValidator(oidc.issuer)`.
- Tokens signed by the discovered JWKS are still rejected if their `iss` does not exactly match `OIDC_ISSUER`.

## Why this changed

- The previous decoder wiring validated timestamps only.
- The issuer validator had been left out of the active validator chain.
- That left the service accepting any correctly signed, unexpired token from the discovered JWKS, even if the token came from an unexpected issuer.
- The current configuration restores single-issuer enforcement.

## Coverage

- Unit coverage in `src/test/java/uk/gov/hmcts/reform/ccd/documentam/configuration/SecurityConfigurationTest.java` checks valid issuer, invalid issuer, and expired token behaviour at validator level.
- Decoder exception coverage in `src/integrationTest/java/uk/gov/hmcts/reform/ccd/documentam/configuration/SecurityConfigurationIT.java` checks decoder-level issuer failures with the active decoder and signed test JWTs.
- Integration coverage in `src/integrationTest/java/uk/gov/hmcts/reform/ccd/documentam/controller/CaseDocumentAmControllerIT.java` exercises authenticated endpoint rejection when a token carries unexpected `iss`.

## Test and pipeline verification

- Focused tests cover valid issuer, invalid issuer, and expired token cases.
- The integration harness mints a signed JWT with explicit `iss` in `src/integrationTest/java/uk/gov/hmcts/reform/ccd/documentam/BaseTest.java`.
- WireMock-backed OIDC discovery and JWKS responses are provided by `src/integrationTest/resources/wiremock-stubs/idam/`.
- Integration test config in `src/integrationTest/resources/application-itest.yaml` keeps `issuer-uri` and `oidc.issuer` aligned for the test runtime.
- A single build-integrated verifier acquires a real IDAM access token and compares its `iss` claim to `OIDC_ISSUER` before smoke and functional runs.
- Local runs skip this live check unless `VERIFY_OIDC_ISSUER=true`.
- Jenkins sets `VERIFY_OIDC_ISSUER=true` and exports `OIDC_ISSUER` explicitly for the verifier.

## Operational guidance

- Do not invent `OIDC_ISSUER`.
- Resolve it from a real access token for the target environment and keep it aligned with the value enforced by the application.
- In this repo, Helm already separates `IDAM_OIDC_URL` from `OIDC_ISSUER` in `charts/ccd-case-document-am-api/values.yaml`.
- Because the verifier runs in the build container before deployed app env is available, Jenkins and Helm issuer values must stay aligned.
- If external services still send tokens with a different issuer, this change will reject them with `401` until configuration or token issuance is aligned.
- For local running, `IDAM_OIDC_URL` should usually point to `http://localhost:5000`, while `OIDC_ISSUER` must exactly match the `iss` claim in the local access tokens being used.

## How to derive OIDC_ISSUER

Derive `OIDC_ISSUER` from a real access token for the target environment. Do not infer it from the public OIDC discovery URL.

Example:

1. Acquire a real bearer token for the same caller path your tests or clients use.
2. Split the JWT on `.` and take the second part, which is the payload.
3. Decode the payload and read the `iss` claim.
4. Set `OIDC_ISSUER` to that exact value.

Python example:

```python
import base64
import json
import sys

payload = sys.argv[1]
payload += '=' * (-len(payload) % 4)
print(json.loads(base64.urlsafe_b64decode(payload))["iss"])
```

## Optional future variant

Only switch to multi-issuer validation if real traffic genuinely needs both values during migration. In that case, use an explicit allow-list validator rather than disabling issuer validation again.
