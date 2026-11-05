# MailBox UI Redesign Assessment

Date: 2026-11-04. Scope: visual/UX only. SMTP, MIME, HTML render, CLI unchanged.

## Current structure

| Area | Class | Notes |
|---|---|---|
| Shell | `InboxView` | Header + sidebar/list/reader splits + status |
| Header | `InboxHeader` | Brand, status, port, start/stop, many equal buttons |
| Sidebar | `InboxSidebar` | Flat toggle buttons, counts |
| List | `MessageListPanel` | Search, sort, dense rows |
| Reader | `MessageReaderPanel` | Many text buttons; HTML via `HtmlEmailView` |
| Header meta | `MessageHeaderPanel` | Subject + From/To/Cc |
| Theme | `Themes` + FlatLaf | Light/dark already present |

## Visual problems

1. Header crowds SMTP and inspection actions equally.
2. Reader toolbar is a long strip of text buttons.
3. Attachments are horizontal chips that wrap poorly.
4. List lacks avatars/initials; styling is ad hoc colors.
5. Split bias gives list more weight than the reading pane.

## Design rules (enforced)

1. **Reading pane gets the most space** (list ~320px default, resizeWeight toward reader).
2. **Do not restyle HTML email content** — only chrome around it.
3. **SMTP is secondary** — compact status; Start/Stop primary by state; no dominance.

## Compatibility risks

- Keep `LegacyControls` wiring for port/start/stop/logs.
- Keep `MessageActions` API.
- Keep FlatLaf; centralize colors in `UiTokens`.
- Do not change `HtmlRenderer` / preprocessor security.
