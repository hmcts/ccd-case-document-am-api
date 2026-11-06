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
upload creator, a future expiry, no withdrawn grant and current bound-case access.
After attachment, actual case/document permissions take over. Attachment checks
the controlled document's binding, expiry, withdrawn state and hash. Existing
unmarked consumers retain their policy.

`DELETE /cases/documents/{id}/pending-access` withdraws only a pending grant; it
never deletes file content. The caller needs CREATE service permission and must
be the upload owner. An attached document is unchanged, subject to its current
case/document read permission. This allows an owner to withdraw pending access
after losing access to the case without returning document data.

Withdrawal requires DM's additive `PATCH /documents/{id}/metadata` endpoint,
which preserves expiry and existing metadata under a row lock. There is no
fallback to bulk PATCH: an older DM rejects the operation safely. Until the
coordinated backend versions are available, leave the consuming toolkit's
pending-access capability disabled.

A browser abandoning an upload before receiving its ID cannot explicitly withdraw
that grant; the document remains private and subject to normal expiry. Saved
draft references must not be withdrawn merely because a browser's edited value
changes or a save response is lost. This API models document-resource access; it
does not add a CCD event-token cancellation ledger.

Validation commands:

```sh
./gradlew test checkstyleMain checkstyleTest
```

Full deployed integration is required with the matching DM and toolkit changes.
