# Testing

## Unit tests

```
mvn test
```

These run headless. They cover:

- The original configuration, i18n, mail save/delete, and UI model tests.
- SMTP start, stop, and start again.
- Parsing plain text, a malformed body, and a `cid:` image.
- The overdue-approval HTML sample: table text, blocked remote image, no script, SVG replaced by a PNG, and a rendered image that contains the table and chart colors (`target/overdue-approval.png`).
- Inbox search, unread filter, sort, read state, delete, and duplicate files.
- Attachment names that try to leave the chosen folder.
- Notifications when disabled, when the window is focused, when a preview is included, and when the desktop backend throws.

## Integration tests

```
mvn verify
```

`SendEmailsIT` sends mail to port 2525. It starts an embedded server when that port is free, and uses a server that is already running otherwise.

`PackagedJarIT` launches `target/MailBox.jar` with `-s -b` and checks that a message is saved. Background mode does not open the window.

## Manual check

```
java -jar target/MailBox.jar -p 2525 -s
```

Point a mailer at `localhost:2525`. Confirm the message appears, the HTML view matches the source, plain and raw views open, and a notification appears when the window is not focused.
