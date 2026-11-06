# Desktop notifications

## Providers

| Priority | Provider | When |
|----------|----------|------|
| 1 | WinRT Action Center toast (`WindowsToastNotifier` via PowerShell) | Windows 10/11 |
| 2 | AWT `SystemTray` balloon | Tray available |
| 3 | `notify-send` | Linux |
| Always optional | In-app toast | Settings |

SMTP reception never waits on notification delivery. Failures are logged only.

## Content

- Title: New email received
- Optional: From, Subject, short preview
- Privacy mode hides subject and preview
- Settings: enable, desktop, in-app, show sender, show subject, preview, privacy, sound, notify when focused, toast duration

## Activation

- Tray icon click / in-app Open: bring MailBox to front and `reveal(messageId)`
- WinRT toast click activation without a custom protocol is **limited**; tray + in-app remain the reliable open path

## Isolation

`WindowsToastNotifier` is platform-specific and is not referenced from the mail model.
