# JWT issuer validation

## Service

`ccd-case-document-am-api`

## Summary

- JWT issuer validation is enabled in the active `JwtDecoder`.
- OIDC discovery and issuer enforcement are configured separately on purpose.
- The enforced issuer must be taken from a real access token `iss` claim, not inferred from discovery metadata or deployment naming.
- `OIDC_ISSUER` remains the single primary issuer; `OIDC_ALLOWED_ISSUERS` is an optional exact-match migration allow-list.
- See [HMCTS Guidance](#hmcts-guidance) for the central policy reference.

## HMCTS Guidance

- [JWT iss Claim Validation guidance](https://tools.hmcts.net/confluence/spaces/SISM/pages/1958056812/JWT+iss+Claim+Validation+for+OIDC+and+OAuth+2+Tokens#JWTissClaimValidationforOIDCandOAuth2Tokens-Configurationrecommendation)
- Use that guidance as the reference point for service-level issuer decisions and configuration recommendations.

## Quick Reference

| Topic | Current repo position |
| --- | --- |
| Validation model | Single primary issuer with optional migration allow-list |
| Discovery source | `spring.security.oauth2.client.provider.oidc.issuer-uri` |
| Enforced issuer | `oidc.issuer` / `OIDC_ISSUER` |
| Optional migration issuers | `oidc.allowed-issuers` / `OIDC_ALLOWED_ISSUERS` |
| Repo wiring | Helm and Jenkins currently use explicit issuer values |
| Runtime rule | `OIDC_ISSUER` or an optional allowed issuer must exactly match the token `iss` claim |

## Discovery vs enforced issuer

| Setting | Purpose | Notes |
| --- | --- | --- |
| `spring.security.oauth2.client.provider.oidc.issuer-uri` | OIDC discovery and JWKS lookup | The service uses it to load OIDC metadata and keys |
| `oidc.issuer` / `OIDC_ISSUER` | Primary enforced token issuer | The active `JwtDecoder` validates the token `iss` claim against this value |
| `oidc.allowed-issuers` / `OIDC_ALLOWED_ISSUERS` | Optional additional migration issuers | Comma-separated values. Each configured value must exactly match a token `iss` claim |

These values can differ. Discovery can point at the public IDAM OIDC endpoint while enforcement pins the exact `iss` emitted in real access tokens.

## Runtime behavior

- `SecurityConfiguration.jwtDecoder()` builds the decoder from `issuer-uri`.
- The decoder then applies both `JwtTimestampValidator` and an exact issuer validator seeded by `oidc.issuer`.
- If `oidc.allowed-issuers` is blank, only `oidc.issuer` is accepted.
- If `oidc.allowed-issuers` is configured, those values are accepted as exact additional migration issuers alongside `oidc.issuer`.
- Tokens signed by the discovered JWKS are still rejected if their `iss` does not exactly match `OIDC_ISSUER` or a configured allowed issuer.

## Why this changed

- The previous decoder wiring validated timestamps only.
- The issuer validator had been left out of the active validator chain.
- That left the service accepting any correctly signed, unexpired token from the discovered JWKS, even if the token came from an unexpected issuer.
- The current configuration restores primary issuer enforcement with an optional exact-match migration allow-list.

## Coverage

| Test area | Coverage |
| --- | --- |
| `src/test/java/uk/gov/hmcts/reform/ccd/documentam/security/OidcIssuerConfigurationTest.java` | Fallback to primary issuer, optional allowed issuer parsing, blank and duplicate allowed issuer handling, and blank primary issuer rejection |
| `src/test/java/uk/gov/hmcts/reform/ccd/documentam/configuration/SecurityConfigurationTest.java` | Primary issuer with an empty allowed list, optional allowed issuer, exact-match rejection, missing `iss`, and expired token behaviour at validator level |
| `src/integrationTest/java/uk/gov/hmcts/reform/ccd/documentam/configuration/SecurityConfigurationIT.java` | Decoder-level allowed issuer, missing `iss`, and issuer failure coverage with the active decoder and signed test JWTs |
| `src/integrationTest/java/uk/gov/hmcts/reform/ccd/documentam/controller/CaseDocumentAmControllerIT.java` | Authenticated endpoint acceptance for an allowed issuer and rejection when a token carries unexpected `iss` |

## Test and pipeline verification

- Focused tests cover primary issuer acceptance with an empty allowed list, allowed issuer acceptance, invalid issuer, missing `iss`, near-match issuer, blank and duplicate allowed issuer parsing, and expired token cases.
- The integration harness mints a signed JWT with explicit `iss` in `src/integrationTest/java/uk/gov/hmcts/reform/ccd/documentam/BaseTest.java`.
- WireMock-backed OIDC discovery and JWKS responses are provided by `src/integrationTest/resources/wiremock-stubs/idam/`.
- Integration test config in `src/integrationTest/resources/application-itest.yaml` keeps `issuer-uri` and `oidc.issuer` aligned for the test runtime.
- A single build-integrated verifier acquires a real IDAM access token and compares its `iss` claim to `OIDC_ISSUER` before smoke and functional runs.
- `OIDC_ALLOWED_ISSUERS` does not currently affect that verifier; update `JwtIssuerVerificationApp` before expecting the build verifier to accept additional migration issuers.
- Local runs skip this live check unless `VERIFY_OIDC_ISSUER=true`.
- Jenkins sets `VERIFY_OIDC_ISSUER=true` and exports `OIDC_ISSUER` explicitly for the verifier.

## Operational guidance

- Do not invent `OIDC_ISSUER`.
- Resolve it from a real access token for the target environment and keep it aligned with the value enforced by the application.
- In this repo, Helm already separates `IDAM_OIDC_URL` from `OIDC_ISSUER` in `charts/ccd-case-document-am-api/values.yaml`.
- Use `OIDC_ALLOWED_ISSUERS` only as a temporary migration allow-list when real traffic genuinely needs additional token issuers that differ from `OIDC_ISSUER`.
- Set `OIDC_ALLOWED_ISSUERS` to comma-separated exact `iss` claim values copied from real accepted access tokens.
- Do not set `OIDC_ALLOWED_ISSUERS` to `IDAM_OIDC_URL`, an OIDC discovery URL, a prefix, a wildcard, a guessed realm URL, or a trailing-slash variant.
- This setting is temporary because each additional issuer broadens the set of trusted token issuers; remove it once traffic has converged on `OIDC_ISSUER`.
- Because the verifier runs in the build container before deployed app env is available, Jenkins and Helm issuer values must stay aligned.
- If the verifier needs to accept an additional migration issuer, update `JwtIssuerVerificationApp` and set `OIDC_ALLOWED_ISSUERS` in Jenkins as well as deployment env.
- If external services still send tokens with a different issuer, this change will reject them with `401` until configuration or token issuance is aligned.
- For local running, `IDAM_OIDC_URL` should usually point to `http://localhost:5000`, while `OIDC_ISSUER` must exactly match the `iss` claim in the local access tokens being used.
- Use [HMCTS Guidance](#hmcts-guidance) as the central policy reference for service-level issuer decisions.

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

## Optional migration allow-list

Leave `OIDC_ALLOWED_ISSUERS` unset unless a specific environment receives valid migration traffic with an issuer that differs from `OIDC_ISSUER`. If real traffic genuinely needs additional issuer values during IDAM migration, set `OIDC_ALLOWED_ISSUERS` to a comma-separated list of exact `iss` values copied from those real access tokens.

This does not replace `OIDC_ISSUER`; it adds temporary additional issuers alongside the single primary issuer at runtime. The value should contain only additional issuers that need temporary acceptance. Including `OIDC_ISSUER` is redundant because the application always accepts the primary issuer.

Each configured additional issuer broadens the set of trusted token issuers. Remove `OIDC_ALLOWED_ISSUERS` once token issuance and callers have converged on the primary `OIDC_ISSUER`.

This setting does not currently change the build-integrated functional/smoke verifier. If that verifier must accept the same additional issuer before smoke or functional setup, update `src/functionalTest/java/uk/gov/hmcts/ccd/documentam/befta/JwtIssuerVerificationApp.java` to read `OIDC_ALLOWED_ISSUERS` and configure the variable in Jenkins.

## Acceptance Checklist

Before merging JWT issuer-validation changes, confirm all of the following:

- The active `JwtDecoder` is built from `spring.security.oauth2.client.provider.oidc.issuer-uri`.
- The active validator chain includes both `JwtTimestampValidator` and exact issuer validation seeded by `oidc.issuer`.
- There is no disabled, commented-out, or alternate runtime path that leaves issuer validation off.
- `issuer-uri` is used for discovery and JWKS lookup only.
- `oidc.issuer` / `OIDC_ISSUER` is used as the enforced token `iss` value only.
- `oidc.allowed-issuers` / `OIDC_ALLOWED_ISSUERS`, if set, contains only explicit additional `iss` values and does not replace `OIDC_ISSUER`.
- `OIDC_ISSUER` is explicitly configured and not guessed from the discovery URL.
- App config, Helm values, preview values, and CI/Jenkins values are aligned for the target environment.
- If `OIDC_ISSUER` changed, it was verified against a real token for the target environment.
- If the build-integrated verifier must accept additional migration issuers, `JwtIssuerVerificationApp` reads the same exact allow-list and Jenkins exports it.
- There is a test that accepts a token with the expected issuer.
- There is a test that accepts a token with an optional allowed issuer.
- There is a test that rejects a token with an unexpected issuer.
- There is a test that rejects a token with a missing `iss` claim.
- There is a test that rejects a token with a near-match issuer.
- There is a test that rejects an expired token.
- There is decoder-level coverage using a signed token, not only validator-only coverage.
- At least one failure assertion clearly proves issuer rejection, for example by checking for `iss`.
- CI or build verification checks that a real token `iss` matches `OIDC_ISSUER`, or the repo documents why that does not apply.
- Comments and docs do not describe the old insecure behavior.
- Any repo-specific difference from peer services is intentional and documented.

Do not merge if any of the following are true:

- issuer validation is constructed but not applied
- only timestamp validation is active
- `OIDC_ISSUER` was inferred rather than verified
- Helm and CI/Jenkins issuer values disagree without explanation
- only happy-path tests exist

## Configuration Policy

- `spring.security.oauth2.client.provider.oidc.issuer-uri` is used for OIDC discovery and JWKS lookup only.
- `oidc.issuer` / `OIDC_ISSUER` is the primary enforced JWT issuer and must match the token `iss` claim exactly.
- `oidc.allowed-issuers` / `OIDC_ALLOWED_ISSUERS` is optional, additive, and limited to exact additional migration issuers.
- Do not derive `OIDC_ISSUER` from `IDAM_OIDC_URL` or the discovery URL.
- Production-like environments must provide `OIDC_ISSUER` explicitly.
- Requiring explicit `OIDC_ISSUER` with no static fallback in main runtime config is the preferred pattern, but it is not yet mandatory across all services.
- Local or test-only fallbacks are acceptable only when they are static, intentional, and clearly scoped to non-production use.
- The build enforces this policy with `verifyOidcIssuerPolicy`, which fails if issuer enforcement is derived from discovery config.

## References

- [HMCTS Guidance](#hmcts-guidance)
