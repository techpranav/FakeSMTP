# Email export (MSG / EML)

## Save as Outlook Message (.msg)

- Right-click an email in the inbox list → **Save as Outlook Message (.msg)** (Outlook-style)
- More menu → **Save as Outlook Message (.msg)**
- Writer: **jotlmsg 1.8** (Apache-2.0) on Apache POI — genuine OLE `.msg`, not a renamed EML
- Compiled for **Java 11+** (ULM floor). jotlmsg 3.x HTML-primary body needs JDK 21 and is not used, so one JAR runs on Java 11–21 alike.
- Contents: subject, from, To/CC/BCC, sent/received date, plain-text body, binary HTML body (+ fromhtml RTF), file attachments (Excel/PDF/etc.), and inline embeds
- HTML embeds: SVG charts → PNG `cid:` parts; `data:image…` and CSS background data URLs → `cid:` parts; existing MIME `cid:` images kept
- Independent validation: unit tests reload with POI/jotlmsg and assert headers, HTML body stream, and rasterized chart attachments

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
- ActiveX / VML Office drawings are not rehydrated as live Excel objects (HTML tables and file attachments are preserved)
- Commercial Aspose Email was not introduced
