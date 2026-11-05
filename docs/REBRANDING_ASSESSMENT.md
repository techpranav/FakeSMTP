# MailBox Rebranding Assessment

Assessment of FakeSMTP → **MailBox** branding references before changes.
Date: 2026-11-04. Branch: `feature/modern-ui`.

## Classification legend

| Class | Meaning |
|---|---|
| **A** | Product branding to replace (user-facing name, docs, jar name, notifications) |
| **B** | Technical SMTP terminology to retain (`SMTP`, ports, SubEthaSMTP, `.eml`) |
| **C** | Original upstream attribution to retain (Nilhcem, copyright, BSD/GPL, historical links) |
| **D** | Compatibility-sensitive identifiers (Java packages, main class, legacy config filenames) |

## Summary inventory

### A — Product branding (replace)

| Location | Current | Target |
|---|---|---|
| `pom.xml` name / description / artifactId / finalName / url (fork) | Fake SMTP / fakesmtp / fakeSMTP-… | MailBox / mailbox / MailBox / techpranav/MailBox |
| `configuration.properties` | FakeSMTP title/name | MailBox Desktop / MailBox |
| `i18n/inbox.properties` | FakeSMTP brand, settings, link copy | MailBox |
| `i18n/messages_*.properties` | About dialog “Fake SMTP Server” | MailBox product text + attribution |
| `DesktopNotificationBackend`, `MainPanel` | “FakeSMTP” tray / test toast | MailBox |
| `TestEmailFactory` sample content | FakeSMTP sample | MailBox sample |
| `HtmlPreprocessor` User-Agent | FakeSMTP | MailBox |
| README, docs/*.md | FakeSMTP product narrative | MailBox narrative + origin note |
| Docker image name / README docker examples | fakesmtp | mailbox |
| Site logo / favicon / splash (visual) | Legacy icon | MailBox icon |
| Failsafe system property name | `fakesmtp.jar` | `mailbox.jar` (test wiring only) |

### B — Technical SMTP (retain)

- SubEthaSMTP dependency and package usage
- SMTP server start/stop, ports, bind address, relay domains
- CLI option semantics (`-p`, `-s`, `-b`, `-m`, `-a`, `-o`, `-r`, `-e`)
- `.eml` persistence and memory mode behavior
- Loggers referencing `org.subethamail.smtp…`
- Words “SMTP”, “email”, “mailbox” in technical sense (folder name already)

### C — Upstream attribution (retain)

- `LICENSE.txt` / BSD dual license notices
- `pom.xml` developers (Nilhcem) and contributors
- Class author `@author Nilhcem` and package `com.nilhcem.fakesmtp`
- Historical docs mentioning FakeSMTP as origin
- About dialog link to original project site (keep as “based on FakeSMTP”)
- Comments referencing upstream PRs on GitHub Nilhcem/FakeSMTP

### D — Compatibility-sensitive (retain unless migrating carefully)

| Identifier | Decision |
|---|---|
| Java package `com.nilhcem.fakesmtp` | **Retain** (no mass rename) |
| Main class `com.nilhcem.fakesmtp.FakeSMTP` | **Retain** (manifest Main-Class unchanged) |
| Maven `groupId` `com.nilhcem.fakesmtp` | **Retain** |
| `~/.fakeSMTP.properties` | **Keep reading**; prefer `~/.mailbox.properties` on save |
| `~/.fakeSMTP-ui.properties` | **Keep reading**; prefer `~/.mailbox-ui.properties` on save |
| Thread names `fakesmtp-*`, temp dir `fakesmtp-attachments` | Soft rename to `mailbox-*` (internal only) |
| Sample addresses `@fakesmtp.local` / CID `logo@fakesmtp` | Soft rename to `@mailbox.local` |

## Out of scope

- Rewriting SMTP stack or GUI architecture
- Implementing Unified License Manager SDK (document MAILBOX product code only)
- Force-push or deleting Git history
- Automatic `git remote` URL change without authorization (provide commands after GitHub rename)

## Planned icon/resources

- Replace `src/main/resources/icon.gif` / add `icon.png` (and size variants)
- Point `application.icon.path` at `/icon.png`
- Update site logo / docs icon asset
- Splash: keep mechanism; refresh image if practical
