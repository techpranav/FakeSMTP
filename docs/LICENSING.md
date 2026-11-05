# MailBox licensing

Product: `MAILBOX` · Java 17+ · JAR has **no** `.lic` inside.

## Run with a license

- Double-click `MailBox.jar`, or run `javaw -jar MailBox.jar`
- If no valid license: **Select license…** opens the native Windows file dialog
- Or place `MAILBOX.lic` in `%APPDATA%\UnifiedLicensing\licenses\`
- Or: `java -jar MailBox.jar -Dulm.license.file=C:\path\MAILBOX.lic`

Missing / expired / wrong machine → pick another `.lic` or Exit. No use without a valid license.

## Admin: issue or renew

No IP needed. Binding uses a **machine id**.

```bat
java -jar ulm-generator.jar machine-id

java -jar ulm-generator.jar generate --type CUSTOMER --customer "Name" --product MAILBOX ^
  --valid-until YYYY-MM-DD --out MAILBOX.lic --passphrase-env ULM_PASS
```

| Goal | Flag |
|---|---|
| This PC | (default) |
| Another PC | run `machine-id` there, then `--machine-id <id>` |
| Any PC | `--unbound` |

**Expired / new PC?** Issue a new `.lic` with the same signing key. No app code or trusted-keys change.
