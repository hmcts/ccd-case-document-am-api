# CCD-7882: controlled error responses

## Current support logging policy

Public error responses remain generic. Detailed request failures, including exception causes
and stack traces, are retained at normal WARN/ERROR levels in server logs; DEBUG is optional
additional troubleshooting. Dependency and health diagnostics retain their originating severity
and respect the source logger threshold. Support must not need to enable DEBUG and reproduce
an incident to obtain its original failure details.

This policy supersedes the DEBUG-only descriptions and test expectations in earlier verification
entries below. Those entries describe historical checks. Deployment acceptance still requires
administrator-only log access, suitable retention and verification of logging/Application Insights
collection. Local tests do not establish those infrastructure controls.


Latest CDAM verification (16 October 2026): **256 unit tests and 70 integration tests passed**, no
failures/errors/skips. Main, test and integration Checkstyle passed; functional Java compilation and
resource processing passed; all 53 changed functional JSON fixtures parsed successfully. Security-chain
integration coverage exercises missing S2S, disallowed services, missing/invalid bearer credentials and
upstream IDAM failures with root-log capture. Six TTL integration expectations now assert safe error
text/code and no document identifier. Deployed BEFTA journeys, real servlet error dispatch/interrupted
streams and App Insights ingestion remain environment verification tasks.

Advice error responses retain Spring's original content negotiation; JSON is not forced for clients
requesting only HTML/XML. An executable comparison against the original HEAD advice verified matching
statuses, bodies and content types for those Accept headers. Dependency diagnostics stay at DEBUG
when negotiation falls through to framework handling; propagated failures are logged once. Propagated document-client and permission failures are logged
at ERROR by advice only. Translated datastore failures retain their original cause for advice DEBUG
logging; handled datastore 404s retain a local DEBUG event.

Error `path` values use route templates, or `/` when no safe route is available. Spring Boot fallback
paths use `/`, so neither error representation echoes document identifiers. Security-level bearer
403s retain `Bearer error="insufficient_scope"` when authenticated, otherwise `Bearer`;
service-auth denials keep their existing
header behaviour. ResponseStatusException headers (including Allow) are preserved. Uncommitted filter
failures clear buffered output and stale content length; already committed responses cannot be rewritten.

API responses no longer include exception class names or raw exception messages. Standard HTTP status descriptions replace diagnostic messages. Existing fixed validation messages remain where already defined. Payment validation details contain user-facing validation messages, never exception causes. Advice error bodies include a stable category `code` derived only from the HTTP status, for example `INVALID_REQUEST` or `INTERNAL_ERROR`. Advice/security-handler error/warn logs contain the same code without exception messages or stack traces. Original exceptions are logged only at DEBUG, which can be enabled for the advice/security handler package when needed; log access must stay restricted through the existing platform controls. No existing non-identifying reference was found in these handlers, so no reference is added. Empty security responses and Spring Boot fallback responses retain their existing shapes.

Spring Boot fallback responses explicitly exclude exception, message, binding errors and stack traces, including when a caller requests them with query parameters. HTML error responses use a fixed generic page. Invalid bearer credentials return an empty 401; OAuth invalid-request errors retain 400. Bearer challenges retain safe protocol error codes only; denied access returns 403 without diagnostic text. Token validation, endpoint permissions and document access checks are unchanged.

## Verification and release handoff

Local regression tests cover diagnostic redaction, malformed JSON, invalid UUID conversion, unsupported media/method status codes, and authentication error handlers. Existing relevant tests are also run. These checks do not establish deployment or end-to-end acceptance.

At merge/deployment planning, assign the delivery owner, QA contact, target date and retest environment. After deployment, record the actual revision, environment, UTC timestamp and sanitised response evidence in the tracking ticket. Do not record credentials, document contents or the OFFICIAL-SENSITIVE report. Obtain detailed reproduction material from SecOps through the approved channel.

## Civil ITHC 2026 F6 / PSC-02 (VUL-8410)

This change owns CDAM handling for `/cases/documents` and document resource/version endpoints used through Manage Case `/documentsv2`. Conversion behaviour and UI presentation are outside this change.

Error JSON retains `status`, `error`, `timestamp` and `path`; `exception` is removed and `error` is a controlled HTTP description. Existing downstream mappings remain: HTTP/Feign client exceptions map to 500 except 401; server 500 maps to 502. Explicit ResponseStatusException statuses remain unchanged, including document upload validation statuses. Share this contract with EXUI-5252 before deployment; clients must use status codes rather than parse diagnostic text. Cross-team agreement is not claimed by this change.

QA must exercise both the CDAM API and the Manage Case upload journey:

- Valid PDF, TXT and Office uploads, downloads and document/version requests.
- Malformed multipart/JSON, unsupported and invalid uploads, oversized uploads, and invalid/missing resource IDs.
- Missing/invalid user and service authentication, disallowed services and users without document access.
- Controlled downstream validation, unavailable-service and timeout responses.
- JSON and HTML responses, including diagnostic query parameters: no class names, traces, internal URLs, paths or software details originating in exceptions.
- Authorised server-log diagnostics and existing trace correlation where available; ensure response evidence contains no sensitive payloads.

Deployment evidence and QA sign-off remain release activities. No QA-person or environment testing has been performed by the local automated checks.

## Local verification — 9 October 2026

Branch: `CCD-7882_Verbose_Error_Messages`, based on master `98b0b526`. Results apply to the local working changes; they are not deployment evidence.

```sh
./gradlew test --offline --tests 'uk.gov.hmcts.reform.ccd.documentam.controller.*' --tests 'uk.gov.hmcts.reform.ccd.documentam.security.*' --tests 'uk.gov.hmcts.reform.ccd.documentam.configuration.ErrorPageConfigurationTest' checkstyleMain checkstyleTest --console=plain
```

Result: **95 tests passed**, no failures or skips; `checkstyleMain` and `checkstyleTest` passed. Test reports are generated under `build/reports/tests/test/` and JUnit XML under `build/test-results/test/`.

## Support use of error codes

HTTP status remains unchanged. Advice JSON includes `code`; security filters continue to return empty
401/403 bodies with the code in handler logs. Spring Boot fallback responses retain their existing shape.
These backend changes do not make a frontend display the code automatically.

| Status | Code | Support action |
| --- | --- | --- |
| 400 / 422 | INVALID_REQUEST | Check request format and safe validation messages; correct input. |
| 401 | AUTHENTICATION_REQUIRED | Check sign-in/session; never collect bearer tokens. |
| 403 | ACCESS_DENIED | Check the user's intended permissions; escalate unexpected refusals. |
| 5xx | INTERNAL_ERROR | Escalate with service, time, status and available telemetry correlation. |

Other existing statuses retain distinct codes: RESOURCE_NOT_FOUND (404), METHOD_NOT_ALLOWED (405),
REQUEST_TOO_LARGE (413), UNSUPPORTED_MEDIA_TYPE (415), TOO_MANY_REQUESTS (429).
Other statuses use REQUEST_FAILED. Codes describe categories, not a confirmed root cause. They are defined as `ErrorCode` enum constants
in Java and serialized as the same strings in JSON; HTTP status remains numeric.

Local log-capture regression tests verify these examples at INFO (synthetic requests):

```text
ERROR Request failed: code=INVALID_REQUEST status=400
WARN  Authentication failed: code=AUTHENTICATION_REQUIRED status=401
WARN  Access denied: code=ACCESS_DENIED status=403
ERROR Request failed: code=INTERNAL_ERROR status=500
```

At DEBUG, an additional event contains the same category/status and original exception with stack trace.
Normal handler events contain no exception message or throwable. Collect service/environment, UTC time,
status, code and an existing telemetry operation ID if available. A category is shared by many requests
and cannot identify one incident. No new incident reference is added.

Enable DEBUG temporarily for the advice and security handler loggers only, then restore the previous level.
Diagnostic logs can contain sensitive details; retain existing restricted access. Local tests establish
handler output, not App Insights ingestion, severity filters, correlation or independent agent exception
collection. Validate those in the target environment before relying on the examples in App Insights.

## Local verification — 15 October 2026

Meaningful codes and INFO/DEBUG log capture: **98 focused tests passed**, no failures or skips.
The focused suites and Checkstyle commands above were rerun. This is local evidence, not deployment
or App Insights evidence. Changes remain uncommitted.

## Temporarily enabling diagnostics in App Insights

The original exception message and stack trace are passed to `log.debug(..., exception)`.
Normal ERROR/WARN events contain only the fixed category and HTTP status. Diagnostics are
available only for failures occurring while DEBUG is enabled; earlier details cannot be recovered.

Two thresholds must allow DEBUG:

1. Override Spring logging for these two handler classes in the target environment:

   ```yaml
   logging:
     level:
       uk.gov.hmcts.reform.ccd.documentam.controller.advice.CaseDocumentControllerAdvice: DEBUG
       uk.gov.hmcts.reform.ccd.documentam.security.SafeAuthenticationErrors: DEBUG
   ```

   Merge these entries with the existing `logging.level` configuration. Keep other application
   loggers at their existing levels; do not enable root DEBUG.

2. Allow the Application Insights Java 3.x agent to collect DEBUG events:

   ```text
   APPLICATIONINSIGHTS_INSTRUMENTATION_LOGGING_LEVEL=DEBUG
   ```

   The agent's default collection threshold is INFO. Changing Spring logging alone is insufficient.
   This agent threshold applies to all emitted logs, so review other loggers already set to DEBUG.
   It is not the agent's `selfDiagnostics` setting.

Apply these temporary overrides through the environment's normal deployment process; a restart/rollout
is needed when supplying startup configuration. Confirm the agent is attached and the connection-string
secret referenced by `lib/applicationinsights.json` is mounted. Existing repository configuration does
not establish that a deployed environment has these settings.

Reproduce a controlled failure. With the Java 3.x agent, normal events appear in `traces`, while events
carrying an exception object appear in `exceptions`. In the Application Insights Logs view, for example:

```kusto
union traces, (exceptions | extend message = outerMessage)
| where timestamp > ago(30m)
| where cloud_RoleName == "ccd-case-document-am-api"
| where message contains "code="
| project timestamp, operation_Id, severityLevel, message, itemType
| order by timestamp desc
```

Use `operation_Id` to inspect related request telemetry when correlation is available. In a
workspace table view, use the equivalent `AppTraces`/`AppExceptions` schema. Check ingestion,
filters and sampling if expected events are missing; local log-capture tests do not validate these.

Restore both thresholds to their previous values after diagnosis and confirm DEBUG events stop.
Original diagnostics may contain sensitive details; use existing restricted log access.
No deployed logging settings were changed by this documentation update.

Reference: [Microsoft Java agent logging configuration](https://learn.microsoft.com/en-us/azure/azure-monitor/app/java-standalone-config#autocollect-logging).

## Senior review follow-up

- Preserve OAuth bearer error statuses: an invalid bearer request remains 400 with INVALID_REQUEST
  in handler logs; authentication failures remain 401. Bearer challenges and bodies contain no diagnostics.
- Move original diagnostics from the owned IDAM/document-client/filter failure paths to DEBUG.
  Keep generic operational events at ERROR/WARN. This does not change HTTP status or authentication checks.
- Update functional response fixtures to assert safe codes/messages instead of exception classes,
  internal messages or identifiers. Keep existing status expectations and fixed user-facing validation details.
- The shared ServiceAuthFilter is replaced with an owned filter using the same validator/allowlist
  and 401/403 semantics. Audit failure diagnostics and operational identifiers are available only at DEBUG.

Functional tests require the services and dependencies used by BEFTA. Fixture updates and local unit
checks are not a deployed functional-test pass; run the functional suites in the target test environment.

For those additional owned paths, temporarily add the relevant logger entries alongside the earlier
DEBUG configuration, with the same App Insights threshold, then restore previous values:

```yaml
logging:
  level:
    uk.gov.hmcts.reform.ccd.documentam.security.filters.ExceptionHandlingFilter: DEBUG
    uk.gov.hmcts.reform.ccd.documentam.client.dmstore.DocumentStoreClient: DEBUG
    uk.gov.hmcts.reform.ccd.documentam.client.datastore.CaseDataStoreClientImpl: DEBUG
    uk.gov.hmcts.reform.ccd.documentam.util.ApplicationUtils: DEBUG
```

Downstream HTTP/Feign client-error mappings are restored to their original behaviour (500 except 401).
Explicit ResponseStatusException and existing server-error mappings are unchanged.

Review-fix validation: **153 tests passed**, no failures or skips; Checkstyle and functional-test
Java compilation/resource processing passed. **53 functional JSON fixtures** were updated and
parsed successfully. Functional suites were not executed against deployed services.

## Complete request-path fixes — 16 October 2026

Public health endpoints retain their availability and status codes but use `show-details: never`,
so failed indicators cannot publish exception text through health JSON. Detailed audit exceptions,
service-authentication exceptions and operational user/case/document/service identifiers are moved
from ERROR/WARN into DEBUG; normal events use generic summaries or fixed codes.

The shared S2S auto-configuration bean (`serviceAuthFiler`) is replaced after initialization by
`SafeServiceAuthFilter`, retaining the same ServiceAuthTokenValidator implementation and configured service allowlist,
Bearer normalization, empty 401/403 responses and downstream exception handling semantics.
Filter overrides with distinct bean names remain intact. This wiring depends on the shared bean name and
has a regression test; review it when upgrading the service-auth library.

Request parsing and response serialization failures in CDAM retain 400/INVALID_REQUEST.
Existing client-validation status expectations stay unchanged. Once a streaming response is committed, its HTTP status/body cannot
be replaced; deployment checks must include aborted downloads and telemetry behaviour.

For temporary diagnosis, enable DEBUG only for the relevant owned security, audit, controller or
service logger, together with the App Insights collection threshold described above. Restore both
thresholds after diagnosis. Full deployed BEFTA execution and App Insights ingestion remain
separate release checks. These changes do not promise that every third-party/framework logger
will suppress diagnostics; those still require environment verification.

Additional logger overrides for the complete-path fixes (use only those needed):

```yaml
logging:
  level:
    uk.gov.hmcts.reform.ccd.documentam.security.SafeServiceAuthFilter: DEBUG
    uk.gov.hmcts.reform.ccd.documentam.auditlog.AuditInterceptor: DEBUG
    uk.gov.hmcts.reform.ccd.documentam.auditlog.aop.AuditAspect: DEBUG
    uk.gov.hmcts.reform.ccd.documentam.service.impl.DocumentManagementServiceImpl: DEBUG
```

### Verified local results — 16 October 2026

**224 affected tests passed**, zero failures/errors/skips. `checkstyleMain`, `checkstyleTest`,
`compileIntegrationTestJava` and `compileFunctionalTestJava` passed. Regression coverage includes
public health UP/DOWN response redaction (200/503), S2S 401/403/allowlist/Bearer handling and actual
shared-bean replacement, INFO/DEBUG diagnostic separation, response serialization (subsequently restored to 400), and
preserved 405 Allow / 415 Accept headers.

Health functional fixtures now expect status without component diagnostics. Existing error fixtures
retain their existing HTTP statuses. Integration/functional
sources were compiled; deployed BEFTA execution, real interrupted downloads, secret mounts,
upstream gateways and App Insights capture/sampling still need environment verification.
Changes are uncommitted; no deployment configuration was applied to a running environment.

## Framework failure-path follow-up — 16 October 2026

Health indicators can log their own exceptions at WARN even when public health details are hidden.
`SafeHealthLoggingConfiguration` registers an owned Logback filter for health-indicator logger classes.
It replaces their WARN/ERROR events with a fixed summary, preserving severity, and routes original
messages/throwables to `logging.SafeHealthDiagnosticsFilter` at DEBUG. The filter is removed when
its Spring context closes. Health results and HTTP statuses are unchanged. This application uses
Logback; custom contributors that log through unrelated logger names need separate verification.

For temporary diagnosis, add the following alongside the previously documented App Insights
collection threshold, then restore the prior level:

```yaml
logging:
  level:
    uk.gov.hmcts.reform.ccd.documentam.logging.SafeHealthDiagnosticsFilter: DEBUG
```

Framework-path regression results: **225 affected tests passed**, no failures/errors/skips.
Tests now use real AbstractHealthIndicator implementations (including nested composite contributors)
and capture propagated INFO/DEBUG logs. Public health status/body and existing bearer checks remain green.
Checkstyle and integration/functional compilation are checked separately; deployed functional tests
and App Insights ingestion are still outstanding.


## Ticket-description confirmation — 16 October 2026

Serialization failures retain the original 400/INVALID_REQUEST mapping. Bearer challenges retain only
allowlisted protocol codes (invalid_request, invalid_token, insufficient_scope), without descriptions,
URIs, scopes or identifiers. Dependency diagnostics use owned DEBUG logging; health/dependency filters
respect source logger thresholds and survive logging reloads without duplicate registration. Error
responses use generic messages and route templates instead of document identifiers.

The reviewed branch is `CCD-7882_Verbose_Error_Messages`. Its ErrorCode enum
and mappings were checked against the proposed table. Generic advice/HTML responses, disabled Boot
exception/message/trace/binding output and DEBUG diagnostics implement the main remediation.
This is a source/documentation review, not evidence of deployment, administrator-only log access,
retesting the reported ITHC URLs, or stakeholder approval of the error category names. See the README
for the concise contract and unresolved branch-specific gaps. No implementation changes or test
reruns were made for this documentation confirmation.


## Final status, bearer and dependency hardening

CDAM serialization/conversion failures retain 400/INVALID_REQUEST. Bearer challenges retain only
allowlisted protocol codes; descriptions, URIs, scopes and identifiers stay in DEBUG diagnostics.
Authenticated access denials retain insufficient_scope, while anonymous denials retain Bearer.

SafeDependencyDiagnosticsFilter covers MVC resolvers, HTTP clients, multipart cleanup, health endpoint
warnings and servlet/container diagnostics. Resolver failures use the existing advice summary; other
covered dependency events retain a generic WARN/ERROR summary and original details only at DEBUG.
For temporary dependency diagnosis, use this logger with the existing App Insights DEBUG threshold:

```yaml
logging:
  level:
    uk.gov.hmcts.reform.ccd.documentam.logging.SafeDependencyDiagnosticsFilter: DEBUG
```

Health and dependency filters respect originating logger levels (including OFF). A reset-resistant
listener reinstalls both filters after logging reload, without duplicate registration, and removes
filters/listener when Spring closes. Regression tests cover repeated resets, shutdown, source levels,
severity, original DEBUG causes, MVC unknown URLs and safe bearer protocol codes.

Validation: **256 unit tests and 70 integration tests passed**, no failures/errors/skips; main/test/
integration Checkstyle and functional compilation/resource processing passed. Deployed functional
journeys, upstream error pages, real interrupted streams and App Insights verification remain outstanding.


## Final branch requirement review — 16 October 2026

Reviewed controller/security/fallback paths, unchanged callers, dependency diagnostics and the supplied
error-code table. All normal owned failure logs use safe summaries; original causes remain available
through the documented DEBUG loggers. Production acceptance requires deployed ITHC/BEFTA retesting,
upstream error-page checks, administrator-only log access and App Insights/agent verification.
The README remains support/maintenance guidance without ticket or branch history.

An isolated executable probe compiled the original advice from HEAD. Both general conversion and
response-writing errors originally returned 400. Unsupported method/media and upload-size exceptions
originally reached the generic 500 handler; the draft 405/415/413 mappings have been reverted to
preserve those actual legacy statuses. Generic framework failures now retain 500 while copying required
Allow/Accept headers. Explicit business 400/401/403/404, upstream mappings and ResponseStatusException
statuses are unchanged. Earlier notes promising different mappings are superseded by this comparison.


Final local validation: **273 unit tests and 70 integration tests passed**, no failures,
errors or skips. Main/unit/integration/functional Checkstyle and functional Java compilation passed;
`git diff --check` passed. Integration tests include the configured authentication filters and actual
request paths. Subsequent fixture validation parsed all **147 functional JSON files** successfully:
the extra closing brace in `Standard_Full_Case_Creation_Data.td.json` was removed. The shared
`AM_404_Response` reason is now `Not Found`, correcting the expectation inherited by nine fixtures.
These fixture checks do not constitute a deployed BEFTA pass.

```sh
./gradlew test integration --offline checkstyleMain checkstyleTest checkstyleIntegrationTest checkstyleFunctionalTest compileFunctionalTestJava --console=plain
```

Deployed functional/ITHC retesting, upstream/container responses, administrator-only log access and
App Insights/independent agent instrumentation remain environment acceptance checks. Error category
names are suitable categories; stakeholder approval is not established by the local tests.
Changes remain uncommitted.

## Anonymous HTTP verification — 19 October 2026

`UnauthenticatedHttpIT` starts the real embedded server on a random local port with mocked downstream
services. Four tests pass without credentials: protected JSON/HTML requests return empty 401s, a failing
public health check returns 503 without details, a missing public resource returns safe JSON 500 or
an empty HTML-request 401 through the original secured error dispatch, and malformed HTTP returns 400 without identifiers, exception details or server branding.
Requests also exercise diagnostic query parameters. These checks establish local HTTP behaviour;
deployed gateways, the reported ITHC URLs and App Insights still require environment verification.


## Content-negotiation compatibility — 19 October 2026

Forced JSON for HTML/XML-only clients was removed because it was an unapproved contract change.
Spring negotiation now matches the original advice. A temporary test compiled that original advice
and compared JSON-only routes, unexpected failures and missing resources under both HTML/XML Accept
headers: statuses, bodies and content types matched. Permanent regression coverage asserts the
original empty 406/500 outcomes where applicable, safe logs and one failure event per request.
Public fallback responses can still use the generic safe HTML page; authentication bodies stay empty.

Validation: **273 unit tests and 74 integration tests passed**, no failures/errors/skips. All four
Checkstyle tasks, functional Java compilation and diff checks passed. Deployment verification remains
outstanding; no content-negotiation contract change is being requested for approval.

## Authoritative remediation and support requirement

The supplied CCD-7882 finding concerns unauthenticated disclosure of implementation details
through HTTP error responses. This note covers the remediation and verification for this service.

The remediation requires generic public error pages/responses and states: "The server should
still log the detailed information, but access to the log will be restricted to administrators."
An event reference is optional. It does not prescribe DEBUG-only logging or require support
to restart a service to obtain diagnostics.

Response sanitisation and administrator diagnostics are separate requirements. DEBUG-only
implementation details described elsewhere in this note record the implementation, not an
acceptance criterion from the finding. If production DEBUG is disabled and no other approved
mechanism records the detailed failure, that is a support/acceptance gap: enabling DEBUG after
an incident cannot recover its missing diagnostics. A safe category/status summary alone does
not satisfy the detailed-log requirement.

Before release, verify that detailed failures are recorded and accessible only to administrators,
including the deployed logging/Application Insights configuration, while public responses stay
generic. Preserve useful validation responses and existing protocol behaviour. Select and test
the logging policy against these requirements; do not assume that moving every diagnostic to
ERROR, or enabling DEBUG globally, is the required solution. Existing local test results do not
establish this deployed acceptance. This requirement also applies to the current support logging policy above.

## Support logging verification — 21 October 2026

43 focused tests passed, zero failures/errors/skips. Checks cover the applicable request/error
paths and confirm that public responses remain sanitised while original exception diagnostics
are retained without enabling DEBUG. Java main/test Checkstyle passed.
Administrator-only log access, retention and deployed collection remain environment acceptance checks.

## Logging uniqueness review — 21 October 2026

43 focused tests and main/test Checkstyle passed. Redundant DEBUG exception events have been removed. The regressions require one owned exception event at INFO and DEBUG.

## Full local verification — 22 October 2026

Reproduced the stale DEBUG-only assertion in `SecurityConfigurationIT`. Both authentication integration
tests now reflect the committed support-logging policy: original exception diagnostics remain in one
server-log event at INFO-configured and DEBUG-configured levels; public bodies/challenges remain safe.
No production code or HTTP behaviour changed in this correction.

Fresh execution passed **272 unit tests and 74 integration tests**, with no failures/errors/skips.
Main, unit, integration and functional Checkstyle passed; functional Java compilation/resource processing,
all 147 functional JSON fixtures and diff checks passed. Deployed BEFTA/ITHC, broker-backed Pact and
administrator-only telemetry access/collection remain unverified locally.
