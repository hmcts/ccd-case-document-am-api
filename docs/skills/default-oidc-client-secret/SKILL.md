---
name: default-oidc-client-secret
description: Use when reviewing or changing default OIDC OAuth2 client configuration in this repo, especially hardcoded spring.security.oauth2.client.registration.oidc.client-secret values, unused OAuth2 client registration, or Spring Security resource-server versus client dependency cleanup.
---

# Default OIDC Client Secret

## Workflow

1. Treat resource-server JWT validation separately from OAuth2 client registration.
2. Search for `spring.security.oauth2.client.registration`, `.oauth2Client`, `spring-boot-starter-oauth2-client`, `ClientRegistrationRepository`, and `OAuth2AuthorizedClient`.
3. If no outbound OAuth2 client usage exists, remove the client registration, `.oauth2Client(...)`, OAuth2 client starter, and test-only client registration shims.
4. Keep `spring-boot-starter-oauth2-resource-server`, `JwtDecoder`, and bearer-token tests intact.
5. Verify with `rg "client-secret: internal|spring-boot-starter-oauth2-client|oauth2Client|TestIdamConfiguration"` and focused compile or test tasks.
