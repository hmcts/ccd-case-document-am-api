# Default OIDC Client Secret Configuration

## Finding

`src/main/resources/application.yaml` defined a Spring OAuth2 client registration for `oidc` with both the client id and client secret set to `internal`.

The service uses Spring Security as a resource server for bearer-token JWT validation. No production code was found that uses the configured OAuth2 client registration to obtain outbound tokens, so the default client secret was unused application configuration rather than a required runtime secret.

## Risk

Leaving a hardcoded default client secret in main application configuration creates avoidable security noise and could become a credential risk if future code starts using that client registration in a non-test environment.

## Remediation

The unused Spring OAuth2 client registration and its hardcoded default secret have been removed. The service remains configured as an OAuth2 resource server for inbound caller bearer JWT validation, and the existing S2S token validation/authorization wiring is unchanged.

The unused OAuth2 client starter dependency, `oauth2Client` security configuration, and the test-only IDAM client registration shim were also removed.

## Verification

Check that the default secret and OAuth2 client wiring are absent:

```bash
rg "client-secret: internal|spring-boot-starter-oauth2-client|oauth2Client|TestIdamConfiguration"
```

Run the focused security regression test:

```bash
./gradlew integration --tests uk.gov.hmcts.reform.ccd.documentam.configuration.SecurityConfigurationIT
```

Run the wider test and checkstyle verification:

```bash
./gradlew test integration
./gradlew checkstyleMain checkstyleIntegrationTest
```
