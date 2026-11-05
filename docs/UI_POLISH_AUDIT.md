# MailBox UI Polish Audit

Date: 2026-11-04

## Findings

1. **Window title can show FakeSMTP** — `Configuration.loadFromUserProfile()` overlays *all* keys from `~/.fakeSMTP.properties` / `~/.mailbox.properties`. A saved profile that once contained `application.title` overrides jar defaults. Fix: never let user profile override branding keys; force `JFrame` title after load.

2. **Reading-pane whitespace** — `MessageHeaderPanel` wraps From/To/Cc in `JScrollPane`s with `MAX_ADDRESS_HEIGHT=52`, and stacks From + Date as separate labeled rows under the avatar. That alone adds ~150–200px before the body. Fix: one compact sender row; single-line recipients; expandable details; no per-field scroll panes.

3. **Attachment band** — `attachmentScroll` max height 160 plus title row padding pushes the HTML body down. Fix: tighter cards and prefer preferred height to content.

4. **List rows** — Row height 84 with 10px padding is acceptable; slight tighten only.

5. **Toolbar** — Functional but tagline should read “SMTP Email Inspector” per product polish request.

## Non-goals

No SMTP/MIME/HTML renderer changes. No new UI frameworks.

## Fixes applied

1. Branding keys restored after user-profile load; user settings file only stores port/dir/tray.
2. `JFrame` title forced to `application.title` (`MailBox Desktop`) — verified on live process.
3. Compact header: subject → avatar+sender+date → one-line To/Cc → Show details.
4. Tighter attachment cards; reading-pane top padding reduced.
5. Tagline set to **SMTP Email Inspector**.
6. List row height / avatar / header height tightened via `UiTokens`.

## Validation

- `mvn clean verify` — pass (after enum static-init fix).
- Packaged `MailBox.jar` launched; window title **MailBox Desktop**.
- Screenshots: `docs/images/screenshot-ui-polish.png`, `docs/images/screenshot-reader-polish.png`.

## Remaining limitations

- Short HTML bodies still leave empty space *below* the body (viewport fill, not header gap).
- Sidebar collapse uses the existing JSplitPane one-touch control only.
- Computer-use sidecar was unavailable; screenshots taken via Win32 `CopyFromScreen`.
