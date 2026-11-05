# Complete email screenshot

## Action

More menu → **Capture Complete Email Screenshot**

Default filename: `MailBox_Email_Subject_yyyyMMdd_HHmmss.png`

## Composition

Off-screen composition (`EmailScreenshot`), not `Robot` / visible viewport.

1. Metadata band painted in Swing (Subject / From / To / CC / BCC / Date — only fields that exist)
2. Complete HTML body via `HtmlRenderer.renderFull` (same path as the reader), or plain text if no HTML
3. The two images are stacked into one PNG

CID images follow the same embedding path as the reader. Application theme CSS is not injected into the body.

## Limits

- Max height 20 000 px; taller content fails with a clear error (suggest EML/MSG) rather than a silently truncated “complete” image
- Export is asynchronous on a `SwingWorker`
