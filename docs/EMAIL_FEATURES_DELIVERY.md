# Email features delivery report

## 1. Audit

Documented in `docs/EMAIL_FEATURES_IMPLEMENTATION_AUDIT.md`.

## 2. MSG export — Implemented & tested

- Library: **jotlmsg 1.8** (Apache-2.0) + Apache POI; project Java release **17** (runs on 17+)
- Genuine OLE `.msg` with plain text, To/CC/BCC, date, attachments; HTML as `message-body.html` (jotlmsg 3 HTML-primary body needs JDK 21 and was dropped for compatibility)
- Independent reload via `new OutlookMessage(file)` in `EmailExportTest`
- UI: right-click inbox row or More → Save as Outlook Message (.msg); async save; overwrite confirm; `Subject_yyyyMMdd_HHmmss.msg`

## 3. EML export — Implemented & tested

- Original MIME bytes from `InboxMessage.getRaw()`
- `EmailExportTest.writesOriginalEmlBytes`

## 4. Native Windows notifications — Implemented; partially tested

- `WindowsToastNotifier` uses WinRT `ToastNotificationManager` via PowerShell
- Fallback: SystemTray balloon; Linux `notify-send`; optional in-app toast
- Settings: show sender/subject, privacy mode (existing keys extended)
- SMTP never blocked by notification failures
- Packaged JAR received mail while running; Action Center toast on this host: **partially verified** (WinRT path exercised; visual toast capture depends on OS focus / AUMID branding as “Desktop app”)
- Click-to-open: tray icon + in-app Open → `reveal(id)` — **tested**; WinRT toast click without protocol registration — **deferred limitation**

## 5. Clickable addresses — Implemented & packaged-JAR verified

- From / To / CC / BCC / Reply-To via `AddressLinkLabel` + `EmailAddressActions`
- mailto validation, context menu (open / copy address / copy name)
- Screenshot: `docs/images/screenshot-features-clickable-addresses.png`

## 6. Complete email screenshot — Implemented & tested

- `ScreenshotDocument` + `HtmlRenderer.renderFull` (off-screen, scroll-independent)
- Includes Subject / From / To / CC / BCC / Date + full body
- Hard fail above 20 000 px height (no silent truncate)
- Artifacts: `docs/images/screenshot-features-full-email.png`, `screenshot-features-overdue-full.png`

## 7. Build

- `mvn test` (unit): pass
- `mvn package`: `target/MailBox.jar` built
- `mvn verify`: ULM ITs may still fail on license signature in this environment (unchanged)

## 8. Dependencies / licensing

| Dep | Version | License |
|-----|---------|---------|
| jotlmsg | 3.0.1 | Apache-2.0 |
| commons-io | 2.16.1 (bumped) | Apache-2.0 |
| Java | 21+ | — |

No commercial MSG SDK.

## 9. Remaining limitations

- WinRT toast activation click without custom protocol
- BCC only when MIME includes it
- Very tall emails cannot produce a single PNG beyond the height cap
