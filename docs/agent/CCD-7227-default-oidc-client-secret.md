# CCD-7227 Agent

## Repo State

- Repo: `ccd-case-document-am-api`
- Branch: `CCD-CCD-7227_Default_OIDC_Client_Secret_Configuration`
- Base: `origin/master`
- Worktree: `/Users/ilapatel/Documents/Work/repos/.worktrees/CCD-7227/ccd-case-document-am-api`
- Status when this note was created: changes are intentionally uncommitted.

## Purpose

Remove the hardcoded default OIDC OAuth2 client secret from main application configuration.

The key finding is that this service validates inbound bearer JWTs as an OAuth2 resource server. No production code was found that uses the configured OAuth2 client registration to obtain outbound tokens, so the `client-secret: internal` value was unused configuration and could be removed with the unused OAuth2 client wiring. The existing IDAM user-info lookup and S2S token generation wiring are unchanged.

## Changes Made

- Removed `spring.security.oauth2.client.registration.oidc.client-id` and `client-secret` from `src/main/resources/application.yaml`.
- Removed `spring-boot-starter-oauth2-client` from `build.gradle`.
- Removed `.oauth2Client(...)` from `src/main/java/uk/gov/hmcts/reform/ccd/documentam/configuration/SecurityConfiguration.java`.
- Removed `TestIdamConfiguration` from `src/integrationTest/java/uk/gov/hmcts/reform/ccd/documentam/BaseTest.java`.
- Deleted `src/test/java/uk/gov/hmcts/reform/ccd/documentam/TestIdamConfiguration.java`.
- Added `src/integrationTest/java/uk/gov/hmcts/reform/ccd/documentam/configuration/SecurityConfigurationIT.java` to verify bearer JWT resource-server authentication still works without OAuth2 client registration.
- Added `docs/security/default-oidc-client-secret.md`.
- Added `docs/skills/default-oidc-client-secret/SKILL.md`.

## Resume Guidance

Do not reintroduce OAuth2 client registration unless new code genuinely needs to acquire tokens as an OAuth2 client. Keep resource-server JWT validation separate from OAuth2 client concerns.

Useful checks:

```bash
rg -n "client-secret: internal|spring-boot-starter-oauth2-client|\.oauth2Client\(|TestIdamConfiguration|ClientRegistrationRepository|OAuth2AuthorizedClient" --glob '!docs/**'
git diff --check
./gradlew integration --tests uk.gov.hmcts.reform.ccd.documentam.configuration.SecurityConfigurationIT
./gradlew test integration
```

## Verification Already Run

- `./gradlew integration --tests uk.gov.hmcts.reform.ccd.documentam.configuration.SecurityConfigurationIT` passed.
- `./gradlew test integration` passed.
- `./gradlew checkstyleIntegrationTest` passed.
