FakeSMTP
========

FakeSMTP is a free fake SMTP server with a modern inbox GUI for testing email in applications.
It is written in Java and uses SubEthaSMTP.

Configure your application to use `localhost` (default port **25**) as the SMTP server. Every message is intercepted, saved as `.eml`, and shown in the inbox with HTML rendering, search, and desktop notifications.

Licensed under BSD or GPL, whichever you prefer.


Requirements
------------

- **Java 8 or newer** to run the jar (also works on Java 11, 17, and 21)
- **JDK 8+ and Maven 3.9+** to build from source

On Unix-like systems you may need root (or another port ≥ 1024) to bind port 25.


Screenshots
-----------

Empty inbox after starting the server on port 25:

![FakeSMTP empty inbox](docs/images/screenshot-inbox-empty.png)

HTML email rendered in the reading pane (tables, links, and charts):

![Rendered overdue-approval email](docs/images/overdue-approval.png)

Desktop notification (in-window toast; tray balloon is also used when the OS supports it):

![New mail notification toast](docs/images/screenshot-notification.png)


Build the jar
-------------

From the project root:

```bash
mvn clean package
```

That runs unit tests, builds the executable fat jar, and (with `mvn verify`) also runs integration tests.

Output file:

```text
target/fakeSMTP-2.1-SNAPSHOT.jar
```

Skip tests only if you must:

```bash
mvn clean package -DskipTests
```

Recommended (full check):

```bash
mvn clean verify
```


Download the jar
----------------

### From a local build

Copy the file Maven produced:

```text
target/fakeSMTP-2.1-SNAPSHOT.jar
```

### From GitHub Releases / Actions (when published)

1. Open the repository on GitHub.
2. Download `fakeSMTP-2.1-SNAPSHOT.jar` (or the release asset with the same name).
3. Keep Java 8+ installed on the machine that will run it.

### From this branch (developers)

```bash
git clone https://github.com/techpranav/FakeSMTP.git
cd FakeSMTP
git checkout feature/modern-ui
mvn clean package
```


Run the jar
-----------

Double-click the jar if your desktop supports it, or:

```bash
java -jar fakeSMTP-2.1-SNAPSHOT.jar
```

Start listening immediately on port 25:

```bash
java -jar fakeSMTP-2.1-SNAPSHOT.jar -s
```

Custom port and output folder:

```bash
java -jar fakeSMTP-2.1-SNAPSHOT.jar -s -p 2525 -o received-emails
```

Background mode (no GUI — SMTP only):

```bash
java -jar fakeSMTP-2.1-SNAPSHOT.jar -s -b -p 2525 -a 127.0.0.1 -o received-emails
```

Memory mode (do not write `.eml` files):

```bash
java -jar fakeSMTP-2.1-SNAPSHOT.jar -s -m
```

Help:

```bash
java -jar fakeSMTP-2.1-SNAPSHOT.jar --help
```

Point your application SMTP host to `localhost` and the same port shown in the header (use **Copy host:port** to copy `localhost:25`).


Inbox features
--------------

- Mail-client layout: Inbox, Unread, All Emails, Attachments, SMTP log
- Search, newest/oldest sort, refresh (`F5`)
- HTML / plain text / headers / raw MIME views
- Safe HTML: no JavaScript, remote images blocked until you allow them
- Tables, links, CID images, SVG charts (rasterized)
- Desktop notifications (tray + in-window toast) with optional sound
- **Test desktop notification** in Settings
- Copy message, open emails folder, mark all read (`Ctrl+Shift+A`), delete (`Delete`)
- Light / dark theme

Settings live in `~/.fakeSMTP-ui.properties`. Port and output directory stay in `~/.fakeSMTP.properties`.

See [`docs/FEATURES.md`](docs/FEATURES.md), [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md), [`docs/TESTING.md`](docs/TESTING.md), and [`docs/BUILD_AND_RELEASE.md`](docs/BUILD_AND_RELEASE.md).


Notifications
-------------

Notifications are **on by default**.

1. Toggle **Notifications** in the header, or open **Settings**.
2. Click **Test desktop notification** to verify tray / toast on your machine.
3. When a real email arrives, FakeSMTP shows an in-window toast and, when available, a system tray balloon (or `notify-send` on Linux).
4. Click the toast or tray icon to focus FakeSMTP and open that message.

Optional: include a short preview, play a beep, or notify even while the window is focused.


Integration tests
-----------------

```bash
mvn verify
```

`SendEmailsIT` starts an embedded server on port 2525 when needed.
`PackagedJarIT` launches the built jar in background mode and checks that mail is saved.


Usage on Docker
---------------

Build from source (`src/main/docker/Dockerfile` uses Java 8):

```bash
mvn package docker:build -DskipTests
docker run -ti -d -p 250:25 --privileged=true -v /home/fakesmtp/mail:/output fakesmtp
```

The root `Dockerfile` still downloads the upstream 2.0 zip.


Alternatives
------------

**[SMTP4dev](http://smtp4dev.codeplex.com/)** — open source, Windows only (.NET).

**[DevNull SMTP](http://www.aboutmyip.com/AboutMyXApp/DevNullSmtp.jsp)** — lightweight, closed source, Java 1.4.


Contact
-------

Use the GitHub nickname (at) gmail (dot) com for the original author.
For this fork, open issues on the repository.
