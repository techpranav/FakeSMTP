# MailBox Rebranding — Final Audit

Date: 2026-11-04  
Branch: `feature/modern-ui`  
GitHub repository: [techpranav/MailBox](https://github.com/techpranav/MailBox) (renamed from FakeSMTP)

## Product identity

| Property | Value |
|---|---|
| Product name | MailBox |
| Desktop title | MailBox Desktop |
| Tagline | Capture. Preview. Inspect. |
| Description | Local Email Testing & Inspection Studio |
| Executable | `MailBox.jar` |
| Maven artifactId | `mailbox` |
| Maven groupId | `com.nilhcem.fakesmtp` (retained) |
| Main class | `com.nilhcem.fakesmtp.FakeSMTP` (retained) |
| ULM product code | `MAILBOX` |

## Files modified (high level)

### Branding / resources
- `src/main/resources/configuration.properties`
- `src/main/resources/i18n/inbox.properties`
- `src/main/resources/i18n/messages_*.properties`
- `src/main/resources/icon.png` (+ size variants), replaced `icon.gif`
- `src/main/resources/META-INF/splash.png`
- `src/site/site.xml`, site logo copy
- `docs/images/mailbox-icon.jpg`

### Application code (branding only; SMTP behavior unchanged)
- `InboxHeader` — brand + tagline
- `DesktopNotificationBackend`, `MainPanel` — MailBox notifications
- `Configuration`, `UISettings` — MailBox config paths with legacy FakeSMTP file read
- `TestEmailFactory`, `TestEmailSender`, `HtmlPreprocessor`, `AttachmentFiles`, `InboxBridge`, `InboxView`
- `PackagedJarIT` — `mailbox.jar` system property

### Build / packaging
- `pom.xml` — artifactId `mailbox`, finalName `MailBox`, docker image `mailbox`, URL updated
- `src/main/docker/Dockerfile` — `MailBox.jar`

### Documentation
- `README.md`, `docs/FEATURES.md`, `docs/ARCHITECTURE.md`, `docs/BUILD_AND_RELEASE.md`, `docs/TESTING.md`
- `docs/ULM_INTEGRATION.md` (new)
- `docs/REBRANDING_ASSESSMENT.md` (new)
- `docs/ENHANCEMENT_ASSESSMENT.md` marked historical

## Remaining FakeSMTP references (classified)

| Kind | Examples | Status |
|---|---|---|
| 1. Upstream attribution | About dialog “Based on FakeSMTP by Nilhcem”, README origin, LICENSE, `@author Nilhcem`, Nilhcem PR links | **Intentional** |
| 2. Historical migration docs | `REBRANDING_*`, `ENHANCEMENT_ASSESSMENT.md` | **Intentional** |
| 3. Technical compatibility | Package `com.nilhcem.fakesmtp`, class `FakeSMTP`, legacy `~/.fakeSMTP*.properties` loaders | **Intentional** |
| 4. Accidental user-facing branding | None found in inbox strings, window title, notifications, jar help, current README product name | **None** |

## GitHub

- Repository renamed: `techpranav/FakeSMTP` → `techpranav/MailBox`
- Local `origin` updated to `https://github.com/techpranav/MailBox.git`
- History preserved (rename only; no force-push)

### Local folder name (optional)

The working directory may still be named `FakeSMTP` on disk. After pulling, you can rename the folder manually:

```text
rename the folder FakeSMTP → MailBox
```

## CLI compatibility

All original options remain: `-o`, `-s`, `-p`, `-b`, `-r`, `-m`, `-a`, `-e`.

Help banner: `usage: java -jar MailBox.jar [OPTION]...`

## Configuration compatibility

| File | Behavior |
|---|---|
| `~/.mailbox.properties` | Preferred write target |
| `~/.fakeSMTP.properties` | Still loaded |
| `~/.mailbox-ui.properties` | Preferred write target |
| `~/.fakeSMTP-ui.properties` | Still loaded when MailBox UI file absent |

## ULM

Documented in `docs/ULM_INTEGRATION.md`. No SDK dependency added. Product code `MAILBOX` in `configuration.properties`.

## Tests executed

| Command | Result |
|---|---|
| `mvn clean verify` | **PASS** (unit + failsafe, including `PackagedJarIT` and `SendEmailsIT`) |
| `java -jar target/MailBox.jar --help` | **PASS** — shows `MailBox.jar` usage |
| Jar contains `icon.png`, `configuration.properties`, `FakeSMTP.class` | **PASS** |

## Acceptance checklist

- [x] Product name is MailBox
- [x] Desktop title is MailBox Desktop
- [x] Product tagline updated
- [x] Application icon updated
- [x] About dialog updated (with upstream attribution)
- [x] Notification branding updated
- [x] Maven artifact is mailbox
- [x] Executable JAR is MailBox.jar
- [x] CLI help updated (dynamic jar name)
- [x] Documentation updated
- [x] GitHub references updated; repo renamed
- [x] ULM product code MAILBOX documented
- [x] SMTP functionality preserved (verify ITs)
- [x] Existing CLI options work
- [x] Existing tests pass
- [x] Packaged JAR launches help successfully
- [x] Upstream attribution preserved
- [x] No unnecessary package/class renaming
