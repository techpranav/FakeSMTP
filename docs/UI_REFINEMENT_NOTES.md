# MailBox final UI refinement notes

Baseline: commit `d2a7fd0` on `feature/modern-ui`.

## Components refined

- Status bar (`InboxView.statusBar`) — label-based storage chrome
- Top toolbar (`InboxHeader`) — consistent control height / grouping
- Message header (`MessageHeaderPanel`) — typography + recipient/details row
- Attachment cards (`MessageReaderPanel`) — name/size hierarchy + hover
- Email list (`MessageListPanel`) — unread/selection/indicators
- Tokens (`UiTokens`) — spacing, hover, preview muted color

## Visual defects corrected

- Oversized editable-looking storage path replaced with:
  folder icon · Storage: short-path · Open Folder · Change · count · Clear all
- Attachment filename/size overlap reduced (two-line meta)
- List horizontal overflow from FlowLayout marks removed
- Toolbar server vs application controls separated with a vertical divider
- Reading header keeps compact spacing; body still gets remaining viewport

## Branding

Unchanged from prior polish: window title remains **MailBox Desktop**.

## Screen captures actually taken

| File | Size |
|------|------|
| `docs/images/screenshot-ui-refine-1280x720.png` | 1280×720 |
| `docs/images/screenshot-ui-refine-1366x768.png` | 1366×768 |
| `docs/images/screenshot-ui-refine.png` | 1400×820 |
| `docs/images/screenshot-ui-refine-maximized.png` | 1938×1038 (primary maximize) |
| `docs/images/screenshot-reader-refine.png` | reader crop |
| `docs/images/screenshot-ui-refine-dark.png` | dark theme attempt |

Not physically tested on this machine: native 1920×1080 and 2560×1440 desktop modes (primary display logical size ~1536×864). Maximized capture is the closest large-window validation.

## Remaining limitations

- Short HTML bodies still leave empty space below the body (viewport fill).
- Toast overlays can cover the lower-left status area briefly.
- List hover quick-actions were not added (keeps rows uncluttered).
