---
name: ccd-case-document-am-security-jwt-issuer
description: Use when working in the HMCTS `ccd-case-document-am-api` repository on JWT issuer validation, OIDC discovery versus enforced issuer configuration, Helm or Jenkins OIDC_ISSUER settings, build-integrated issuer verification, or related regression testing.
---

# Security JWT Issuer

## Overview

Use this skill for JWT issuer validation changes in `ccd-case-document-am-api`.

## Workflow

1. Check current state with `git status --short` and inspect local diffs before editing.
2. Review [`src/main/java/uk/gov/hmcts/reform/ccd/documentam/configuration/SecurityConfiguration.java`](../../../src/main/java/uk/gov/hmcts/reform/ccd/documentam/configuration/SecurityConfiguration.java), [`src/main/resources/application.yaml`](../../../src/main/resources/application.yaml), [`charts/ccd-case-document-am-api/values.yaml`](../../../charts/ccd-case-document-am-api/values.yaml), and [`Jenkinsfile_CNP`](../../../Jenkinsfile_CNP).
3. Confirm the split between discovery and enforcement:
   `spring.security.oauth2.client.provider.oidc.issuer-uri` is for discovery and JWKS.
   `oidc.issuer` / `OIDC_ISSUER` is the enforced issuer matched against the token `iss` claim.
4. Search for `issuer`, `issuer-uri`, `JwtDecoder`, `JwtIssuerValidator`, `JwtTimestampValidator`, `OIDC_ISSUER`, and `VERIFY_OIDC_ISSUER` before changing behavior.
5. Keep coverage focused across three layers:
   validator-level tests in [`src/test/java/uk/gov/hmcts/reform/ccd/documentam/configuration/SecurityConfigurationTest.java`](../../../src/test/java/uk/gov/hmcts/reform/ccd/documentam/configuration/SecurityConfigurationTest.java),
   decoder exception tests in [`src/integrationTest/java/uk/gov/hmcts/reform/ccd/documentam/configuration/SecurityConfigurationIT.java`](../../../src/integrationTest/java/uk/gov/hmcts/reform/ccd/documentam/configuration/SecurityConfigurationIT.java),
   and endpoint integration coverage in [`src/integrationTest/java/uk/gov/hmcts/reform/ccd/documentam/controller/CaseDocumentAmControllerIT.java`](../../../src/integrationTest/java/uk/gov/hmcts/reform/ccd/documentam/controller/CaseDocumentAmControllerIT.java).
6. Preserve the repo’s build-integrated issuer verification path:
   use [`src/integrationTest/java/uk/gov/hmcts/reform/ccd/documentam/BaseTest.java`](../../../src/integrationTest/java/uk/gov/hmcts/reform/ccd/documentam/BaseTest.java),
   [`src/integrationTest/resources/application-itest.yaml`](../../../src/integrationTest/resources/application-itest.yaml),
   [`src/functionalTest/java/uk/gov/hmcts/ccd/documentam/befta/JwtIssuerVerificationApp.java`](../../../src/functionalTest/java/uk/gov/hmcts/ccd/documentam/befta/JwtIssuerVerificationApp.java),
   and the WireMock OIDC stubs under [`src/integrationTest/resources/wiremock-stubs/idam/`](../../../src/integrationTest/resources/wiremock-stubs/idam/)
   rather than duplicating token-minting or issuer-resolution helpers.
7. Keep issuer terminology for config and docs, and `iss` terminology only for claim-level assertions or validator and decoder messages.
8. Do not guess `OIDC_ISSUER`. Keep Helm and any test-time issuer values aligned with the `iss` claim used by real tokens for the target environment.

## References

- Primary repo guidance: [`docs/security/jwt-issuer-validation.md`](../../../docs/security/jwt-issuer-validation.md)
- Security workflow: [`docs/skills/security/SKILL.md`](../security/SKILL.md)
