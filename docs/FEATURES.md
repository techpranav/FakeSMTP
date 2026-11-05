# Features

The original SMTP test server is unchanged: configurable port and bind address, output directory, memory mode, background mode, auto-start, relay domains, and the external `.eml` viewer.

## Inbox

The window is a mail client layout: header, sidebar, message list, and reading pane.

- Sidebar: Inbox, Unread, Flagged, All Emails, Attachments, and the SMTP log. Each mailbox shows a count.
- List: sender, subject, preview, time, unread emphasis, flag star, attachment mark. Search covers sender, recipient, subject, and preview. Sort by newest, oldest, subject, sender, or flagged first.
- Reading pane: HTML, plain text, headers, and raw MIME. Mark read or unread, flag/unflag, delete one message, open the saved `.eml`.
- Export: right-click inbox row or More menu → **Save as MSG** (Outlook `.msg` with HTML also attached as `message-body.html`), **Save as EML**, and **Save screenshot** (full email PNG with metadata + body).
- **Send test email** in the header injects a sample multipart message (HTML, links, table, chart, inline image, `sample.txt` / `sample.csv`) so you can check the UI without an SMTP client.
- Attachments: name, type, size, Content-ID. Save (names are reduced to a single safe file name), open with the system application on an explicit click, and preview images.
- Empty states for no mail, no search hits, no unread mail, no flagged mail, no attachments, and nothing selected.
- Clear all still asks before deleting saved files.

## HTML

HTML and multipart messages are shown like a desktop mail client: tables, links, inline CSS, forms (display-only, controls disabled), `cid:` images, data-URL images, and SVG charts (drawn as PNG). The reading pane scrolls and reflows when the window is resized. Plain text is available beside the HTML. A broken message falls back to plain text or the raw source and does not stop the server.

JavaScript is not executed. Form submit and active controls are disabled. Remote images stay blocked until the user clicks **Load images** or enables them in settings. `file:` and `javascript:` URLs are not loaded. Only `http`, `https`, and `mailto` links open in the browser.

The overdue-approval sample renders like this:

![Rendered overdue-approval email](images/overdue-approval.png)

## Notifications

On by default. A new message shows the sender, subject, and a short preview (the preview can be hidden). MailBox always shows an in-window toast, and also uses the system tray balloon on Windows/macOS when available (or `notify-send` on Linux). Clicking the toast or tray icon focuses MailBox and opens that message. Settings include **Test desktop notification**. A sound is optional and off by default.

Notifications are skipped when the window is focused unless **Notify even when MailBox is focused** is on. A notification failure does not affect delivery.

## Extra tools

- **Copy host:port** copies `localhost:25` (or the current port) for app SMTP config.
- **Copy** copies headers and body text from the selected message.
- **Open folder** opens the directory where `.eml` files are saved.
- **Mark all read**, `F5` refresh, and `Delete` remove the selected message after confirm.

## Themes and settings

Light is the default. Dark is available from the header. Settings are saved in `~/.mailbox-ui.properties` (legacy FakeSMTP UI settings files are still read).

## Limitations

- The HTML renderer supports CSS 2.1 (tables, inline styles, borders, basic layout). It does not apply flexbox, grid, or media queries.
- SVG is converted to a bitmap, so it is not selectable text.
- Emoji depend on fonts installed on the machine.
- A tray click selects the most recent notified message.
- Read/unread and flag state are not kept across restarts.
- Outlook `.msg` export uses jotlmsg: subject, recipients, plain text, and attachments are written; the HTML body is included as the `message-body.html` attachment (the Java 8 jotlmsg API does not embed HTML as the primary RTF/HTML body).
- Screenshots capture the prepared HTML body only (not the full Swing chrome). Very tall messages are capped at 20 000 px height.
- The original root `Dockerfile` still downloads the upstream 2.0 zip. The image built from source uses Java 8.
