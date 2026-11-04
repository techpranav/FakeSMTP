# Build and release

## Requirements

- JDK 21 or newer (developed on Temurin 21.0.12).
- Maven 3.9 or newer.
- No Node.js, browser, or extra download at runtime.

## Build

```
mvn package
```

The assembly plugin writes one executable jar:

```
target/fakeSMTP-2.1-SNAPSHOT.jar
```

Run it with:

```
java -jar target/fakeSMTP-2.1-SNAPSHOT.jar
```

The jar contains the application, Swing UI, FlatLaf, Flying Saucer, jsoup, jsvg, and SubEthaSMTP. There are no platform native libraries.

`mvn verify` runs unit tests, builds the jar, then runs the integration tests, including the packaged-jar check.

## Docker image from source

`src/main/docker/Dockerfile` uses `eclipse-temurin:21-jre` and the jar produced by `mvn package`. The root `Dockerfile` is the original one that downloads the upstream 2.0 zip; it is not the Java 21 build.

## What is preserved

Command-line options, `.eml` file names, memory mode, background mode, and the SMTP server behavior are the same. UI preferences go to `~/.fakeSMTP-ui.properties` and do not replace `~/.fakeSMTP.properties`.
