# Responsive UI validation

Follow-up to dark-theme work on `feature/dark-theme-ui`.

## Notification overlay

- Toast is **not** in `BorderLayout`. It is a child of a **null-layout `JLayeredPane`**.
- Workspace (`JSplitPane`) always fills the layered pane (`setBounds(0,0,w,h)`).
- Toast is positioned bottom-right only when visible; it never reserves a full-width strip.
- Compact card (`UiTokens.TOAST_WIDTH` × ~108): MailBox icon, title, sender/subject/preview, Open, Dismiss.
- Queue while a toast is showing; title consolidates to “N new emails”.
- Dismiss does not mark read / Open selects the message. Click on the card also opens.
- Timers stop on dismiss; next queued toast starts a new timer.
- Packaged JAR: overlay sits on top of the reading pane; inbox/splitters/status bar do not move.

## Email list

- Cell width is locked to the viewport (`JList.setFixedCellWidth`).
- Horizontal scrollbar is `AS_NEEDED` but normal rows do not exceed the viewport.
- Width-aware ellipsis via `TextFit` for sender, subject, preview.
- Date and flag/attachment marks keep a reserved trailing width.
- Tooltips expose full sender/subject/preview.
- Confirmed at a 1280×720 window: dates remain visible; sender/subject truncate; no list horizontal scrollbar.

## Search / sort

- Single row: flexible search (placeholder, search icon, clear) + compact sort (~136px) + icon refresh with tooltip.
- Search minimum width ~96px.
- Combined search + sort covered by `InboxModelTest.searchAndSortCombine`.

## Attachments

- Flow layout wraps; card width adapts to reading-pane width.
- Filename truncation via `TextFit.ellipsizeChars`; full name in tooltip.
- Preview / Save / Open remain on the card.

## HTML email

- Theme CSS is not injected (`EmailRenderTest` asserts no `mailbox-theme` / chrome hex).
- Viewport may reflow to available width; if the laid-out document is **wider** than the viewport, horizontal scrolling is enabled.
- Overdue-approval fixture remains a permanent regression sample (`src/test/resources/emails/overdue.html`).
- Wide-table fixture: `src/test/resources/emails/wide-table.html`.

## Theme switching

- Light and Dark chrome remain intact.
- HTML body stays on the independent white email surface.
- Theme apply is deferred with `SwingUtilities.invokeLater` so FlatLaf does not repaint a disposed combo popup.

## Resolutions actually captured (packaged JAR)

Primary display: 1920×1080 physical / ~1536×864 logical.

| Shot | File |
|------|------|
| Dark inbox + compact overlay (queued) | `docs/images/screenshot-responsive-toast-multi.png` |
| Light inbox/reader + overlay at 1280×720 | `docs/images/screenshot-responsive-1280x720.png` |
| Light attachments + HTML | `docs/images/screenshot-responsive-attachments.png` |
| Overdue HTML render fixture | `docs/images/overdue-approval.png` / `screenshot-responsive-overdue.png` |

Native exclusive modes 1600×900 / 1920×1080 / 2560×1440 were **not** switched on the monitor. 1280×720 was applied with `MoveWindow` on the packaged UI.

At 1280×720: status bar visible, search/sort/refresh fit, toast does not reserve workspace, list has no unnecessary horizontal scrollbar.

## Tests

- `TextFitTest`
- `InAppToastTest` (dismiss, queue, open, compact size)
- `InboxModelTest.searchAndSortCombine`
- `EmailRenderTest` overdue + wide-table fixtures

`mvn test`: 39 tests, 0 failures.

`mvn verify`: unit tests pass; `SendEmailsIT` passes. `MailBoxUlmAgentIT` / `PackagedJarIT` fail in this environment because the ULM agent rejects the generated `MAILBOX.lic` (`INVALID_SIGNATURE` / unknown key). The packaged JAR **does** start when launched with `-Dulm.license.file=ulm/MAILBOX.lic`.
