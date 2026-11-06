# Clickable email addresses

## Where

Reading-pane header (`MessageHeaderPanel`):

- From (primary sender link)
- To (summary row)
- CC / BCC / Reply-To (Show details)

Only the address (or display name) is interactive — not the entire metadata row.

## Behaviour

- Left click → `mailto:` via `Desktop.mail` / `browse`
- Address validated before opening
- Right click → Open / Copy email address / Copy display name
- Tooltip shows the full `Name <addr>` form
- Accent colour + underline on hover; works in Light and Dark

HTML body links continue to use `SafeLinks` (http/https/mailto only).
