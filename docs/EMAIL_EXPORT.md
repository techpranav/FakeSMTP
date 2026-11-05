# Email export (MSG / EML)

## Save as Outlook Message (.msg)

- Right-click an email in the inbox list → **Save as Outlook Message (.msg)** (Outlook-style)
- More menu → **Save as Outlook Message (.msg)**
- Writer: **jotlmsg 1.8** (Apache-2.0) on Apache POI — genuine OLE `.msg`, not a renamed EML
- Compiled for **Java 17+** (ULM floor). jotlmsg 3.x HTML-primary body needs JDK 21 and is not used, so one JAR runs on Java 17 and 21 alike.
- Contents: subject, from, To/CC/BCC, sent/received date, plain-text body, HTML as `message-body.html` attachment, file attachments, inline images as attachments
- Independent validation: unit test reloads the file with `new OutlookMessage(file)` and asserts recipients + HTML attachment

## Save as EML (.eml)

- More menu → **Save as EML (.eml)**
- Writes the original RFC822/MIME bytes from `InboxMessage.getRaw()`

## Save dialog

- Default name: `Subject_yyyyMMdd_HHmmss.msg` (or `.eml`)
- Invalid filename characters sanitized
- Overwrite confirmation
- Export runs on a `SwingWorker` (not the EDT)

## Limitations

- BCC only when present in the received MIME
- Commercial Aspose Email was not introduced
