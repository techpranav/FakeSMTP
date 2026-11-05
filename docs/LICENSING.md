# MailBox licensing

Product: `MAILBOX` · Java 17+ · JAR has **no** `.lic` inside.

## Run with a license

Place `MAILBOX.lic` in `%APPDATA%\UnifiedLicensing\licenses\`  
or start with `-Dulm.license.file=C:\path\MAILBOX.lic`  
or pick a file when MailBox shows **Select license…**.

Missing / expired / wrong machine → select a new license or Exit. No use without a valid license.

## Admin: issue or renew

No IP address is needed. Binding uses a **machine id**.

```bat
java -jar ulm-generator.jar machine-id

java -jar ulm-generator.jar generate --type CUSTOMER --customer "Name" --product MAILBOX ^
  --valid-until YYYY-MM-DD --out MAILBOX.lic --passphrase-env ULM_PASS
```

- Default: bound to **this** PC.
- Other PC: run `machine-id` there, then `--machine-id <id>`.
- Any PC: `--unbound`.

**Expired?** Generate a new `.lic` with the same signing key. Do **not** change app code or `trusted-keys.properties` (only re-export keys if you rotate signing keys).
