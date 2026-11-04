# FakeSMTP — Enhancement Assessment (historical)

> Historical planning document written before the MailBox rebrand. Product name is now **MailBox**; technical package layout remains as described.

## Decision update

After this assessment, the project was developed on Temurin 21 but the packaged jar is compiled to **Java 8** bytecode so it runs on Java 8 and every newer JDK. The source Docker image uses `eclipse-temurin:8-jre`. HTML is still rendered with Flying Saucer; SVG charts in the sample message are rasterized with jsvg because Flying Saucer does not draw SVG.

This assessment was written before any source change on the `feature/modern-ui` branch.
It describes the code base as found on `master` (commit `a487607`, version `2.1-SNAPSHOT`).

## 1. Existing architecture

FakeSMTP is a small single-module Maven project (~40 classes). It embeds a SubEthaSMTP 3.1.7
server, writes each received message to an `.eml` file and shows it in a Swing window built
with MigLayout. Components talk to each other through `java.util.Observable`/`Observer`.

| Package | Responsibility | Key classes |
|---|---|---|
| `com.nilhcem.fakesmtp` | Entry point | `FakeSMTP` |
| `core` | CLI parsing, configuration, i18n | `ArgsHandler`, `Configuration`, `I18n` |
| `core.exception` | Port/host errors, uncaught exception dialog | `BindPortException`, `UncaughtExceptionHandler`, ... |
| `server` | SMTP server and message persistence | `SMTPServerHandler`, `MailListener`, `MailSaver`, `SMTPAuthHandler(Factory)` |
| `model` | Presentation model and message model | `UIModel`, `EmailModel` |
| `gui` | Window, menu, tray popup, folder chooser | `MainFrame`, `MainPanel`, `MenuBar`, `DirChooser`, `TrayPopup` |
| `gui.info` | Header widgets | `PortTextField`, `StartServerButton`, `SaveMsgField`, `NbReceivedLabel`, `ClearAllButton` |
| `gui.tab` | Tabs | `MailsListPane` (table), `LogsPane` (SMTP log), `LastMailPane` (raw text of last mail) |
| `gui.listeners` | Menu / window actions | `AboutActionListener`, `ExitActionListener`, `MainWindowListener` |
| `log` | Logback appender redirecting SubEtha session logs to the UI | `SMTPLogsAppender`, `SMTPLogsObservable` |

## 2. Execution flow

```
FakeSMTP.main(args)
 ├─ ArgsHandler.handleArgs(args)            (commons-cli GnuParser; sets UIModel save path/relay/host)
 │    └─ ParseException → displayUsage()    (help output, exit)
 ├─ -s and -b  → SMTPServerHandler.startServer(port, bindAddress)   (no GUI, process stays alive on server thread)
 └─ otherwise  → EDT: set Mac dock icon, system L&F → new MainFrame()
                     ├─ MenuBar (File/Exit, Edit/Messages location, Help/About)
                     ├─ MainPanel
                     │    ├─ registers observers (see §4)
                     │    ├─ builds MigLayout UI (port, start button, save dir, counter, 3 tabs, Clear all)
                     │    └─ checkArgs(): -p sets port, -s toggles StartServerButton, -m disables save field
                     ├─ restores port / output dir from ~/.fakeSMTP.properties
                     └─ close(): saves port + dir to ~/.fakeSMTP.properties, stops server
```

## 3. SMTP receiving lifecycle

1. `SMTPServerHandler` (enum singleton) owns one `SMTPServer`, wrapped in a `SimpleMessageListenerAdapter`
   around `MailListener`. Auth is accepted for any credentials (`SMTPAuthHandler`).
2. SubEthaSMTP calls `MailListener.accept()` (always `true`) for each RCPT, then
   `MailListener.deliver(from, recipient, data)` **once per recipient**, on a SubEtha session thread.
3. **`MailSaver.saveEmailAndNotify(from, to, data)`** is the exact method called for every received email:
   - drops the message if `--relay-domains` is set and the recipient does not match;
   - reads the stream as UTF-8, skipping the first 4 lines (SubEtha `Received:` header lines);
   - extracts the subject with a regex;
   - inside `synchronized (getLock())`: writes the file (unless memory mode), sets received date and
     path on the `EmailModel`, then `setChanged(); notifyObservers(model)`.
4. `SMTPServer.start()` can only be called once per instance (`IllegalStateException: SMTPServer can only be
   started once`). This is why the original UI disables the Start button permanently and has no Stop button.

## 4. How the UI is notified

`MainPanel.addObservers` registers `NbReceivedLabel`, `MailsListPane`, `LastMailPane` and `ClearAllButton`
as observers of `MailSaver`. **Observers run synchronously on the SMTP thread and touch Swing components
directly** — a pre-existing Swing threading violation, and any observer exception propagates into the SMTP
transaction. `ClearAllButton` notifies the same panes (plus `LogsPane`) on the EDT while holding the
`MailSaver` lock.

`MailsListPane` maintains `UIModel.listMailsMap` (row → file path); `MailSaver.deleteEmails()` relies on it,
so this component must stay wired even if hidden.

## 5. Email storage lifecycle

- Format: the raw RFC 822/MIME message (minus the 4 SubEtha lines) with platform line separators, stored as
  `<output-dir>/<ddMMyyhhmmssSSS>[n].eml` (`emails.suffix=.eml`). The file is written with the platform default
  charset (`FileUtils.writeStringToFile` without charset).
- Default directory: `received-emails` (relative to the working directory), overridable by `-o`, the
  folder chooser, or the saved user configuration.
- Memory mode (`-m`): no file is written (`filePath == null`), file actions are disabled.
- Deletion: only via *Clear all → Yes*, which deletes the files listed in `UIModel.listMailsMap`.

## 6. MIME and attachment handling

There is no MIME parsing. Only the `Subject:` line is extracted (and decoded with `MimeUtility.decodeText`
in `MailsListPane`). Attachments, HTML and encodings are not interpreted; *Last message* shows raw text, and
double-clicking a row opens the `.eml` with the OS default program or the `--eml-viewer` executable.
JavaMail 1.4.4 (`javax.mail:mail`) is already on the classpath as a SubEthaSMTP dependency.

## 7. Command-line arguments

| Option | Effect |
|---|---|
| `-o, --output-dir` | Output directory |
| `-s, --start-server` | Start the server at launch |
| `-p, --port` | SMTP port |
| `-a, --bind-address` | Bind address |
| `-b, --background` | No GUI (only together with `-s`) |
| `-r, --relay-domains` | Comma separated accepted recipient domains |
| `-m, --memory-mode` | Do not persist emails |
| `-e, --eml-viewer` | External program used to open emails (double click in the mails table) |

Invalid arguments print the commons-cli help.

## 8. Threads

- EDT for the UI; SubEtha `ServerThread` + one session thread per connection (cached thread pool).
- Shutdown hook stops the server. No other background executors.

## 9. Build

- Maven, `maven-compiler-plugin` 3.3 with **source/target 1.6**, `maven-assembly-plugin` `jar-with-dependencies`
  bound to `package` with the custom `META-INF/MANIFEST.MF` (`Main-Class`, splash screen), final name
  `fakeSMTP-${version}.jar`. Failsafe runs `*IT` tests in `integration-test`/`verify`.
- Dependencies: MigLayout 5.0, SubEthaSMTP 3.1.7 (+ JavaMail 1.4.4, JAF 1.1.1), slf4j/logback, commons-io,
  commons-cli, AppleJavaExtensions; tests: JUnit 4.12, commons-email 1.3.3.

## 10. Test baseline (measured before any change)

| Check | Result |
|---|---|
| `mvn clean test` with JDK 16 or 21 | **Fails to compile**: `Source option 6 is no longer supported. Use 8 or later.` |
| Same with source/target raised to 1.8 (temporary POM) | 16 unit tests, 0 failures |
| `mvn verify` (same temporary POM) | 6/6 `SendEmailsIT` errors: they require an externally started server on port 2525 |
| `SendEmailsIT` against the baseline JAR started with `-s -b -p 2525` | 6/6 pass, 7 `.eml` files written |

Both failures are pre-existing and are not regressions.

## 11. Identified extension points

1. **`MailSaver` observers** — the single, already-existing "email received" event. The new inbox registers
   one more observer that immediately hands off to a background parser and then to the EDT.
2. **`ClearAllButton` observers** — the existing "clear" event; the inbox listens to it too.
3. **`SMTPServerHandler.startServer/stopServer`** — making the handler create a fresh `SMTPServer` per start
   enables a real Stop/Start cycle without touching SubEtha.
4. **`StartServerButton` (Observable)** — already notifies `PortTextField`; it can publish server status.
5. **`MainFrame` / `MainPanel`** — the layout container to restyle; all existing widgets can be re-hosted.
6. **`Configuration`** stays as is; UI preferences go to a separate file so the existing file is never altered.

## 12. HTML rendering — evaluation

| Option | HTML/CSS | Swing integration | Packaging | Java | Security control |
|---|---|---|---|---|---|
| Swing `JEditorPane` | HTML 3.2, tiny CSS subset | Native | None | Any | Weak (loads URLs itself) |
| JavaFX `WebView` (OpenJFX) | HTML5/CSS3 (WebKit) | `JFXPanel` bridge, two UI threads | Platform-specific natives per OS/arch (~30–40 MB each); fat JAR for win+mac+linux ≫100 MB; not part of most JDKs | OpenJFX 17+ needs Java 11+, 21+ needs Java 17+ | JS can be disabled; remote loading must be blocked by HTML rewriting/CSP |
| Flying Saucer 9.1.22 + jsoup 1.17 | XHTML + CSS 2.1 (tables, inline/embedded CSS, floats, borders, backgrounds, `max-width`); no flexbox/grid/media queries | Native Swing panel | Pure Java, ~1.7 MB, no natives | Java 6 / Java 8 | No scripting engine at all; **every** image/CSS/XML load goes through a `UserAgentCallback` we implement |

**Recommendation: Flying Saucer + jsoup.** Email HTML is overwhelmingly table-based with inline CSS — the
CSS 2.1 subset that Flying Saucer handles well (it is also roughly what Outlook desktop supports). It keeps
the single cross-platform JAR small, runs on Java 8 (Docker image is `java:8`), avoids a second UI thread,
and gives a hard security boundary: no JavaScript engine exists, and the custom user agent only serves
`cid:` parts, `data:` URIs and — after explicit consent — remote images prefetched off the EDT.
jsoup converts real-world (non-XHTML) markup into a W3C DOM and removes active content.
Limitations to document: no CSS3 layout (flex/grid), no media queries, no web fonts, emoji glyphs depend on
installed fonts.

## 13. Notifications — evaluation

- **AWT `SystemTray` + `TrayIcon.displayMessage`**: native toast on Windows 10/11 (click → `ActionEvent`),
  Notification Center on macOS, balloon on Linux desktops that still provide a tray. Zero dependencies.
- **`notify-send`** (libnotify) as a Linux fallback when there is no system tray (e.g. GNOME); no click action.
- **In-app toast** inside the window as the final fallback and when the window is already focused.
Delivery happens on the EDT after parsing; failures are caught and logged and never reach the SMTP thread.

## 14. Compatibility issues found

1. Java 1.6 source/target cannot be compiled by any current JDK → raise to Java 8 (`--release 8`).
2. Swing access from SMTP threads in existing observers → wrap `MailSaver` observers so they run on the EDT.
3. Observer exceptions can fail SMTP delivery → new observer never throws.
4. `SMTPServer` single-use → recreate the instance on restart.
5. `SendEmailsIT` needs an external server → start an embedded one when port 2525 is free (still works against
   an external instance).
6. New i18n keys would be missing in non-English bundles → `I18n` falls back to English per key.

## 15. Implementation plan

1. Build: Java 8, pin surefire (headless), add FlatLaf (modern L&F, light/dark), Flying Saucer, jsoup;
   `Multi-Release: true` manifest attribute.
2. Server: restartable `SMTPServerHandler`; `UIModel.toggleButton()` really stops the server.
3. Mail layer (`com.nilhcem.fakesmtp.mail`): MIME parser → `ParsedEmail`; HTML preparation/sanitizing;
   safe Flying Saucer user agent; attachment file utilities.
4. Inbox model (`model`): EDT-confined `InboxModel` (folders, search, sort, read state, counts) fed by an
   `InboxUpdater` observer on `MailSaver` (background parse → EDT).
5. GUI: header (status/port/start/stop/notifications/theme/settings), sidebar (Inbox, Unread, All Emails,
   Attachments + SMTP log and the classic panes), email list, reading pane (HTML/Text/Headers/Raw,
   attachments, actions), empty states, status bar (existing save-dir field, counter, Clear all).
6. Notifications (`notification` package) with tray / notify-send / in-app fallbacks and settings.
7. UI settings in `~/.fakeSMTP-ui.properties`.
8. Tests for every layer, embedded-server IT, packaged-JAR IT; manual E2E with the packaged JAR; docs.
