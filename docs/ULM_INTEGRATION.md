# Unified License Manager (ULM) — MailBox product identity

MailBox is prepared for future integration with Unified License Manager.

## Product identity

| Property | Value |
|---|---|
| Product code | `MAILBOX` |
| Product name | MailBox |
| Desktop application | MailBox Desktop |
| Description | Local Email Testing & Inspection Studio |

The product code is also recorded in `configuration.properties` as `application.product.code=MAILBOX`.

## Current state

ULM is **not** embedded in this repository yet.

Do **not**:

- Add an unverified SDK dependency.
- Invent SDK versions or API methods.
- Implement a separate parallel licensing system.
- Embed private signing keys.
- Generate production licenses automatically.
- Introduce machine binding.

## When integrating ULM

1. Use product code **MAILBOX** and product name **MailBox**.
2. Preserve existing SMTP, inbox, CLI, and configuration behavior.
3. Preserve SDK validation, digital signature verification, and offline licensing as provided by ULM.
4. Keep the integration isolated from SubEthaSMTP and the mail receive path so a license check failure cannot corrupt email capture.

## Compatibility note

Java packages remain `com.nilhcem.fakesmtp.*` and the main class remains `com.nilhcem.fakesmtp.FakeSMTP` for binary and documentation continuity with the FakeSMTP lineage. ULM product identity is independent of the Java package name.
