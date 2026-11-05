# MailBox UI Component Guide

## Layout

`InboxView` hosts:

1. `InboxHeader` — brand, compact SMTP status, secondary actions  
2. `InboxSidebar` — Mail / Developer sections  
3. `MessageListPanel` — search, sort, avatar rows  
4. `MessageReaderPanel` — view modes, More menu, header, attachment cards, body  
5. Status bar — save path + received count  

Reading pane is preferred via `LIST_WIDTH=320` and split `resizeWeight=0.28`.

## Design tokens

All chrome colors/spacing live in `UiTokens`. Do **not** apply tokens inside HTML email content (`HtmlEmailView` / Flying Saucer).

## Actions

Reader primary: HTML / Text / Headers / Raw, Mark read, Flag.  
Secondary (More menu): mark all, copy, MSG/EML/screenshot, folder, open, delete.

## SMTP

Status chip shows Running / Stopped / Error with port. Start/Stop remain the same `LegacyControls` buttons; visually quieter than the reading pane.
