# Dark theme audit

Baseline: merged master after PR #2 (`feature/ulm-license-integration`), screenshots from UI refine.

## Root cause

`Themes.install(true)` only switches FlatLaf. MailBox chrome colors live in **static light-only** `UiTokens` fields and are baked into panels at construction time. Sidebar FlatLaf `STYLE` strings also hardcode light hex values (`#EAF2FF`, `#EEF1F5`).

## Components stuck light in Dark mode

| Area | Issue |
|------|--------|
| Sidebar | `SIDEBAR_BG` + selected/hover STYLE hex |
| Email list / rows | `SURFACE`, `SELECTED_*`, borders |
| Reading chrome | `SURFACE` / `APP_BG` header, tools, attachments |
| Status bar | `APP_BG` + label colors |
| Empty states | `SURFACE` / text colors |
| In-app toast | Hardcoded `#E8F0FE` full-width bar |
| Settings labels | `TEXT` from light tokens |
| Hairline borders | Created once with light `BORDER` |
| Avatar fill | Light pastel algorithm |

## Intentionally light (must stay)

| Area | Reason |
|------|--------|
| HTML email body surface | Preserve received email appearance |
| Inline images / SVG / tables | No theme CSS injection |

## FlatLaf coverage (mostly OK)

Menus, native dialogs, combo/text fields follow FlatDarkLaf once installed. Port field already dark in screenshots. Custom panels do not.

## Required fixes

1. Dual light/dark semantic tokens with live `UiTokens.apply(dark)`.
2. `applyTheme()` on inbox chrome after theme switch (no restart).
3. Theme-aware sidebar STYLE strings.
4. Compact toast (not full-width light banner).
5. Keep HTML viewport independent of app theme.

## Fixes applied

- `UiTokens.apply(boolean)` light/dark semantic palettes.
- `Themes.install` updates tokens before FlatLaf refresh.
- `applyTheme()` on sidebar, list, reader, header, status, toast, empty states.
- Compact right-aligned toast with Open/Dismiss.
- Compact sort combo (~136px) with A–Z / Z–A options.
- HTML body remains `EMAIL_SURFACE` (white) in both themes.

## Visual validation (packaged JAR)

Captured:

- `docs/images/screenshot-dark-inbox.png` (1400×820)
- `docs/images/screenshot-dark-1280x720.png`
- `docs/images/screenshot-dark-preview.png`
- `docs/images/screenshot-light-inbox.png`

Confirmed: sidebar/list/reader chrome/status dark; HTML email white surface preserved; live switch to Light intact.

Not physically tested as native modes: 1920×1080, 2560×1440 (primary logical ~1536×864).

## Follow-up (responsive iteration)

See `docs/RESPONSIVE_UI_VALIDATION.md` for toast overlay, list overflow, and attachment wrapping.

Theme isolation still holds: HTML email surface stays white in both Light and Dark. Live switch remains intact; applying the theme is deferred with `SwingUtilities.invokeLater` so FlatLaf does not repaint a disposed combo popup.
