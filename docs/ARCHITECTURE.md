# Architecture

FakeSMTP is still the original SubEthaSMTP test server with a Swing window. The inbox is an extra view on the same receive path.

## Receive path

```
SMTP client
  -> SMTPServerHandler (SubEthaSMTP)
  -> MailListener.deliver
  -> MailSaver.saveEmailAndNotify   (unchanged file format)
  -> observers, including InboxBridge
       -> background thread parses the message
       -> Swing thread updates InboxModel and asks NewMailNotifier
```

`MailSaver` still writes `<output-dir>/<ddMMyyhhmmssSSS>.eml` and still skips the four SubEtha prefix lines. Memory mode (`-m`) still skips the file. Relay domains still drop non-matching recipients before any UI work.

`SMTPServerHandler` creates a new `SMTPServer` after a stop, because one SubEtha instance can only be started once. That is what makes the Stop button work.

## Packages

| Package | Role |
|---|---|
| `server` | SMTP lifecycle, `MailSaver`, `InboxBridge` |
| `mail` | MIME parsing, HTML cleanup, SVG-to-PNG, safe image loading |
| `model` | `EmailModel`, `UIModel`, `InboxModel` |
| `gui.inbox` | Header, sidebar, message list, reading pane, settings |
| `notification` | Tray notification, `notify-send`, in-window toast |
| `core` | CLI, `configuration.properties`, `UISettings` |

## HTML

JavaMail reads the MIME message (including quoted-printable). jsoup removes scripts, event handlers, forms, and external stylesheets. Remote images are removed unless the user allows them, and even then only an HTTP or HTTPS `image/*` response is downloaded, with redirects disabled. SVG elements are drawn to PNG by jsvg and embedded as data URLs. Flying Saucer paints the result. Its user agent serves only `cid:` parts and `data:` images.

Links open only for `http`, `https`, and `mailto`. Forms stay visible with controls disabled so mail that contains approval buttons still looks complete without posting anywhere.

The reading pane tracks the viewport width so HTML reflows when the window is maximized or resized, and the body scrolls independently of the subject and address header.

## Preferences

Server port and the output directory stay in `~/.fakeSMTP.properties`. Inbox preferences (theme, notifications, sort, window bounds) are stored separately in `~/.fakeSMTP-ui.properties`. Read/unread state lasts for the session. Messages loaded from the output directory are marked read and do not notify.

## Threads

SMTP threads only hand a copy of the message to a daemon parser thread. Swing updates run on the event dispatch thread. A parse or notification failure is logged and does not fail the SMTP transaction.
