# MailBox UI Testing

## Automated

```
mvn clean test
mvn verify
mvn package
```

Unit/integration coverage still validates SMTP, parse, export, inbox model. UI is Swing; manual checks cover chrome.

## Manual checklist

1. Launch `java -jar target/MailBox.jar`
2. Title is **MailBox Desktop**; header shows brand + tagline
3. SMTP status chip updates on Start/Stop
4. Send test email → list row with avatar → reading pane HTML unchanged
5. More menu: MSG / EML / screenshot / delete still work
6. Attachment cards: Preview / Save / Open
7. Sidebar sections + counts
8. Theme light/dark
9. Resize window — reader grows more than list
10. Settings sections save without resetting other prefs

## Design rules to verify

- HTML body matches sender styling (no MailBox chrome colors inside body)
- SMTP does not dominate the first glance
- Reading pane is the largest surface
