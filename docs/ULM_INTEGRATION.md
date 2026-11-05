# Unified License Manager — MailBox

MailBox is integrated with ULM through the **zero-code Java Agent** (no application-source changes for
startup enforcement).

See **[LICENSING.md](LICENSING.md)** for product code `MAILBOX`, build, launch, and troubleshooting.

## Summary

| Item | Value |
|---|---|
| Product code | `MAILBOX` |
| Integration | `ulm-agent.jar` + sidecar launchers |
| SDK feature API | Optional; not required for CORE startup protection |
| Private keys in this repo | Never |

Historical note: the Java package namespace remains `com.nilhcem.fakesmtp` for compatibility. ULM does
not use the Java package name as the product code.
