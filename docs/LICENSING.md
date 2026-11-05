# MailBox licensing (ULM)

MailBox uses **Unified License Manager (ULM) 1.1.0** for software licensing.

| Property | Value |
|---|---|
| Product code | `MAILBOX` |
| Product name | MailBox |
| Categories | `INTERNAL` (expiry optional / perpetual) and `CUSTOMER` (expiry mandatory) |
| Features | `CORE`, `HTML_EMAIL_RENDERING`, `DESKTOP_NOTIFICATIONS`, `ATTACHMENT_INSPECTION` |
| Validation | Offline Ed25519 signature verification via the ULM Java Agent |
| Machine binding | None |
| Activation server | None |

Java packages remain `com.nilhcem.fakesmtp.*` and `Main-Class` remains `com.nilhcem.fakesmtp.FakeSMTP`.
ULM product identity is independent of the Java package name.

## Runtime requirement

MailBox application bytecode stays **Java 8**, but the ULM agent/SDK require **Java 17+**.
Launch the licensed distribution with JDK 17 or newer.

## Supported launch path

```
target/ulm-dist/
  MailBox.jar
  ulm/ulm-agent.jar
  ulm/ulm.properties
  ulm/trusted-keys.properties
  run.bat
  run.sh
```

Start with `run.bat` / `./run.sh`. Starting `MailBox.jar` without `-javaagent` bypasses ULM and is
**not** the supported distribution path (see ULM security limitations).

## Build

```bat
rem 1. Export public keys from the ULM generator into:
rem    src\main\ulm\trusted-keys.properties
rem 2. Package with the ULM profile (agent jar from the ULM build):
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
5. Distribute the file to the licensee (for example via SharePoint). Never distribute private keys.

## Troubleshooting

| Symptom | Check |
|---|---|
| Exit code 3 at startup | Missing/invalid license; read the agent message |
| `WRONG_PRODUCT` | License is for MQLX or historical FAKESMTP — need a MAILBOX license |
| `INVALID_SIGNATURE` | Tampered file or wrong public key embedded in the distribution |
| `EXPIRED` | Request a renewal from the administrator |

Old `FAKESMTP` licenses are not accepted by MailBox.
