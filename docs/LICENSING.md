# MailBox licensing

Product code: `MAILBOX`. Java 17+. JAR ships **without** a `.lic` file.

## Use

1. Install `MAILBOX.lic` (or pick it when MailBox asks).
2. Default folder: `%APPDATA%\UnifiedLicensing\licenses\`
3. Override: `-Dulm.license.file=<path>`

Expired / missing / wrong machine → GUI offers **Select license…** or Exit. No use without a valid license.

## Issue a license (admin)

```bat
java -jar ulm-generator.jar machine-id
java -jar ulm-generator.jar generate --type CUSTOMER --customer "Name" --product MAILBOX --valid-until 2027-12-31 --out MAILBOX.lic --passphrase-env ULM_PASS
java -jar ulm-generator.jar export-public-keys --out src\main\ulm\trusted-keys.properties
```

Default generate binds to **this** PC. For another PC: `--machine-id <id>`. Floating: `--unbound`.
