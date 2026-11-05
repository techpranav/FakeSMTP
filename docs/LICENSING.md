# MailBox licensing (ULM)

MailBox uses **Unified License Manager (ULM) 1.1.0** for software licensing.

The application JAR is distributed **without** a license file. Licenses are issued and
delivered separately. On startup MailBox validates a ULM `MAILBOX` license; without a
valid license the application will not start SMTP or open the UI.

| Property | Value |
|---|---|
| Product code | `MAILBOX` |
| Product name | MailBox |
| Categories | `INTERNAL` (expiry optional / perpetual) and `CUSTOMER` (expiry mandatory) |
| Features | `CORE`, `HTML_EMAIL_RENDERING`, `DESKTOP_NOTIFICATIONS`, `ATTACHMENT_INSPECTION` |
| Validation | Offline Ed25519 via embedded `license-sdk` (startup gate in `MailBoxLicenseGate`) |
| Machine binding | None |
| Activation server | None |

Java packages remain `com.nilhcem.fakesmtp.*` and `Main-Class` remains `com.nilhcem.fakesmtp.FakeSMTP`.
ULM product identity is independent of the Java package name.

## Runtime requirement

MailBox requires **Java 17+** (ULM `license-sdk`).

## Startup behaviour

1. On load, MailBox searches for a valid `MAILBOX` license
   (`-Dulm.license.file`, search dirs, then `%APPDATA%\UnifiedLicensing\licenses\MAILBOX.lic`).
2. **GUI mode:** if the license is missing, invalid, or expired, a dialog offers
   **Select license…** (copies a chosen `.lic` into the default license folder) or **Exit**.
3. **Background mode (`-s`) / headless:** prints a clear error to stderr and exits with code `3`
   (no interactive picker). Pass `-Dulm.license.file=<path>` before starting.

Public verification keys live in `src/main/ulm/trusted-keys.properties` (safe to commit —
public only) and are embedded into `META-INF/ulm/` at package time. Private keys and `.lic`
files are never packaged into the JAR and must not be committed.

## Build

```bat
rem 1. Export public keys from the ULM generator into:
rem    src\main\ulm\trusted-keys.properties
rem 2. Package (license-sdk is a normal dependency; .lic is NOT included):
mvn package
```

Optional agent sidecar distribution (in addition to the in-app gate):

```bat
set ulm.agent.jar=..\unifiedlicensemanager\license-agent\target\ulm-agent.jar
mvn -Pulm package
```

Or use `scripts\package-with-ulm.cmd` after building ULM 1.1.0 locally.

## License file location

Default: `%APPDATA%\UnifiedLicensing\licenses\MAILBOX.lic`

Overrides: `-Dulm.license.file=...` or `-Dulm.license.dir=...`

## Administrator workflow

1. Open ULM Generator.
2. Select product **MailBox** (`MAILBOX`).
3. Choose INTERNAL or CUSTOMER; set expiry for CUSTOMER.
4. Generate and validate the `.lic` file.
5. Distribute the JAR and the `.lic` file separately (for example via SharePoint).
   Never distribute private keys.

## Troubleshooting

| Symptom | Check |
|---|---|
| “License not available” dialog / exit code 3 | Missing `.lic`; select a file or place `MAILBOX.lic` in the default folder |
| “License expired” | Request a renewal from the administrator |
| `WRONG_PRODUCT` | License is for MQLX or historical FAKESMTP — need a MAILBOX license |
| `INVALID_SIGNATURE` | Tampered file or wrong public key embedded in the distribution |
| Trusted-key / validation error | Ensure `src/main/ulm/trusted-keys.properties` was present at package time |

Old `FAKESMTP` licenses are not accepted by MailBox.
