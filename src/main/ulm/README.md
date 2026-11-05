# ULM public keys for MailBox

`trusted-keys.properties` in this folder holds **public verification keys only**.

- Safe to commit and ship inside the application JAR (`META-INF/ulm/`).
- Cannot create or forge licenses.
- Never place `*.ulmkey`, passphrases, generator databases, or `.lic` files here.

Export from the ULM License Generator (`export-public-keys`) whenever keys are rotated.
