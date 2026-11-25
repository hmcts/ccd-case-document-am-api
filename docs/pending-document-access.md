# Controlled pending uploads

`POST /cases/documents/pending` creates a private pending upload. This dedicated
route forces controlled access on the server; older deployments reject the route
before storing a document. The ordinary upload route retains its existing policy
and rejects pending-access parameters.

An optional 16-digit `caseId` binds the upload to an existing case. The service
checks the authenticated user's current CCD access and verifies the returned case
reference, case type and jurisdiction. Both buffered and streamed uploads retain
the binding in document-store metadata.

Before attachment, controlled metadata, binary and token access requires the
upload creator, a future expiry and current bound-case access. TTL updates and
deletion also check pending ownership. After attachment, actual case/document
permissions take over; controlled TTL changes require document UPDATE permission
and deletion requires DELETE permission. Attachment checks the document's binding,
case type, jurisdiction, expiry and hash. Existing unmarked consumers retain their
policy.

Cancellation and abandoned-upload behavior retain the existing document expiry
lifecycle. There is no immediate grant-withdrawal API, event-token cancellation
ledger, additional metadata mutation or document deletion on form cancellation.
Pending documents remain private to their creator until expiry; bound-case access
must still be current. Real `case_id` attachment consumes the pending policy and
switches reads to current CCD document authorization.

This implementation uses the existing document-store upload and attachment APIs.
It does not require document-management-store changes. Enable the consuming
client capability only after deploying this CDAM endpoint and verifying the
supported case-type scope. Historical document routing remains unchanged.

Validation commands:

```sh
./gradlew test checkstyleMain checkstyleTest
```

Full deployed integration is required with the matching toolkit/webapp changes.
