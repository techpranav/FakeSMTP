# Unified License Manager — MailBox

MailBox enforces licensing **in-process** with the ULM `license-sdk` at startup
(`com.nilhcem.fakesmtp.license.MailBoxLicenseGate`). The shipped JAR does not contain a
`.lic` file; licenses are supplied separately.

See **[LICENSING.md](LICENSING.md)** for product code `MAILBOX`, build, launch, and troubleshooting.

## Summary

| Item | Value |
|---|---|
| Product code | `MAILBOX` |
| Primary integration | Embedded `license-sdk` + startup gate (GUI license picker) |
| Optional packaging | `ulm-agent.jar` sidecar via `mvn -Pulm package` |
| Private keys in this repo | Never |
| License file in JAR | Never |

Historical note: the Java package namespace remains `com.nilhcem.fakesmtp` for compatibility. ULM does
not use the Java package name as the product code.
