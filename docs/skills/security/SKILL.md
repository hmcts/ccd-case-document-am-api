---
name: ccd-case-document-am-security
description: Use when working in the HMCTS `ccd-case-document-am-api` repository on general Spring Security configuration, IDAM/OIDC integration, security-related regression testing, or other security changes that are not specifically about JWT issuer validation. For JWT issuer validation, use `docs/skills/security-jwt-issuer/SKILL.md`.
---

# Security

## Overview

Use this skill for general security changes in `ccd-case-document-am-api`.
For JWT issuer validation and issuer-wiring work, use [`docs/skills/security-jwt-issuer/SKILL.md`](../security-jwt-issuer/SKILL.md).

## Workflow

1. Check current state with `git status --short` and inspect local diffs before editing.
2. Review the relevant security configuration, runtime wiring, and tests for the change you are making.
3. Search `src/main/java/uk/gov/hmcts/reform/ccd/documentam/configuration/`, `src/main/java/uk/gov/hmcts/reform/ccd/documentam/security/`, `src/test/java/`, and `src/integrationTest/java/` before changing behavior.
4. Check `src/main/resources/application.yaml`, `charts/ccd-case-document-am-api/values.yaml`, and `Jenkinsfile_CNP` if the change affects deployment or environment wiring.
5. If the task turns into JWT issuer-validation work, switch to [`docs/skills/security-jwt-issuer/SKILL.md`](../security-jwt-issuer/SKILL.md).

## References

- JWT issuer-specific guidance: [`docs/skills/security-jwt-issuer/SKILL.md`](../security-jwt-issuer/SKILL.md)
