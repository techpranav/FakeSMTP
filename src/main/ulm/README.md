# ULM public keys for MailBox

Place `trusted-keys.properties` here (exported from the ULM License Generator).

- Public keys only — never place `*.ulmkey`, passphrases, generator databases, or `.lic` files here.
- At `mvn package` time the file is copied into the JAR as `META-INF/ulm/trusted-keys.properties`
  so the in-app license gate can verify signatures offline.
- The same file is required for the optional `mvn -Pulm package` agent distribution.
