# Email features implementation audit

Audit for MSG/EML export, native desktop notifications, clickable addresses, and full email screenshots.

## Existing pipeline

| Concern | Location |
|---------|----------|
| SMTP receive | `SMTPServerHandler`, SubEthaSMTP `MailListener` → `MailSaver` |
| Persist `.eml` | `MailSaver` writes under configured output dir; raw string on `EmailModel` / `InboxMessage.getRaw()` |
| Parse MIME | `EmailParser` → `ParsedEmail` (from/to/cc/bcc/reply-to, subject, plain, html, attachments, inline CID map, headers, preparedHtml) |
| Inbox model | `InboxBridge` → `InboxModel` / `InboxMessage` |
| Reader UI | `MessageReaderPanel` + `MessageHeaderPanel` + `HtmlEmailView` |
| More menu | `MessageReaderPanel` already has Save MSG / Save EML / Screenshot |
| Export API | `EmailExport.saveAsMsg` / `saveAsEml` / `saveScreenshot` |
| Notifications | `NewMailNotifier` → `NotificationBackend` / `DesktopNotificationBackend` + `InAppToast` |
| Settings | `UISettings` + `SettingsDialog` + `SettingsPreferences` |
| Safe links | `SafeLinks` already allows `mailto:` from HTML body |

## Current gaps vs requirements

### MSG export

- Uses **jotlmsg 1.8** (Apache-2.0, POI-based): produces a genuine OLE `.msg`, not a renamed EML.
- jotlmsg 1.x/2.x only support **plain-text primary body**. HTML is currently attached as `message-body.html`.
- jotlmsg **3.0+** adds `setHtmlBody` + inline `contentId`, but requires **JDK 21**.
- Decision: bump `maven.compiler.release` to **21** and jotlmsg to **3.0.1** so HTML is the primary Outlook body. Sent date, recipients, attachments, and CID inlines are wired through.
- Apache POI HSMF remains read-oriented; writing stays on jotlmsg (no commercial Aspose dependency).

### EML export

- Writes `InboxMessage.getRaw()` bytes — preferred path when original MIME was retained.
- Memory mode and load-from-disk keep raw text on the model.

### Desktop notifications

- Labelled “native” but implemented as **AWT SystemTray balloon** (+ Linux `notify-send`) and in-app toast.
- Tray balloons ≠ Windows 10/11 Action Center toasts.
- Click-to-open already works via tray / in-app toast → `InboxView.reveal(id)`.
- Gap: true WinRT toast when minimized / behind other windows; privacy mode for subject/preview; sender toggle.

### Clickable addresses

- Header shows From / To / CC as plain `JLabel` text (HTML details panel is also non-interactive).
- HTML body links already use `SafeLinks` for mailto/http(s).

### Screenshot

- `HtmlRenderer.renderFull` paints the **HTML body only** off-screen (not `Robot` viewport).
- Gap: composed metadata block (Subject / From / To / CC / BCC / Date) above the body; Better filename; large-image guard messaging; EDT offload.

## Integration points (no rewrite)

1. Enhance `EmailExport` (+ optional thin helpers); keep More menu wiring in `InboxView` / `MessageActions`.
2. Add `WindowsToastNotifier` behind `DesktopNotificationBackend`; keep tray + in-app as fallbacks.
3. Replace address labels in `MessageHeaderPanel` with a small hyperlink control + context menu.
4. Compose screenshot HTML in `EmailExport` / `HtmlRenderer` without mutating stored `ParsedEmail` HTML.
5. Extend `UISettings` / `SettingsDialog` for privacy and show-sender flags (reuse existing keys where possible).

## Licensing

| Dependency | License | Role |
|------------|---------|------|
| jotlmsg 3.0.1 | Apache-2.0 | Genuine MSG write |
| Apache POI (transitive) | Apache-2.0 | Compound file |
| Existing FlatLaf / Flying Saucer / jsoup / jsvg | unchanged | UI / HTML |

No commercial MSG libraries introduced.

## Technical limitations (honest)

- WinRT toast **click activation** without registering a custom protocol is limited; MailBox still activates the correct message via tray icon click and in-app toast Open.
- Screenshots of extremely tall emails are capped (`MAX_SCREENSHOT_HEIGHT`); callers get a clear error rather than a silently truncated “complete” claim when rendering fails.
- BCC is only present when the received MIME includes BCC headers (SMTP often omits them).
