# MailBox Design System

Chrome-only tokens. **Rendered email HTML is never restyled by MailBox.**

## Colors

| Token | Hex | Use |
|---|---|---|
| APP_BG / SIDEBAR_BG | `#F5F7FA` | Window chrome |
| SURFACE | `#FFFFFF` | Panels, reader |
| TEXT | `#202939` | Primary labels |
| TEXT_SECONDARY | `#687386` | Meta, previews |
| ACCENT | `#3976D2` | Focus / links in chrome |
| SELECTED_BG | `#EAF2FF` | Selected list/nav |
| BORDER | `#E5E9F0` | Hairlines |
| SUCCESS / WARNING / ERROR | green / amber / red | SMTP status |

Code: `com.nilhcem.fakesmtp.gui.inbox.UiTokens`

## Layout priority

1. Reading pane — majority of width (`list` ~320px, split weight toward reader).
2. Email list — secondary.
3. Sidebar — ~200px.
4. Header — ~64px; SMTP compact on the left of actions, not center-stage.

## Typography

Segoe UI / system UI via FlatLaf. Brand ~18px bold. Subject ~20px. Meta ~12–13px.
