package com.nilhcem.fakesmtp.integration;

import org.junit.Assume;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Launches the packaged MailBox JAR under the ULM Java Agent with a temporary test key/license.
 * Skips when ULM artifacts or MailBox.jar are not present (so normal FakeSMTP CI stays green).
 * Written for Java 8 bytecode compatibility with the MailBox test classpath.
 */
public class MailBoxUlmAgentIT {

    @Test
    public void agentBlocksStartupWithoutLicenseAndAllowsWithValidMailboxLicense() throws Exception {
        Path mailboxJar = Paths.get(System.getProperty("mailbox.jar", "target/MailBox.jar")).toAbsolutePath().normalize();
        Path ulmRoot = Paths.get("..", "unifiedlicensemanager").toAbsolutePath().normalize();
        Path agent = ulmRoot.resolve("license-agent/target/ulm-agent.jar");
        Path cli = ulmRoot.resolve("license-cli/target/ulm-cli.jar");
        Path generator = ulmRoot.resolve("license-generator/target/ulm-generator.jar");
        Assume.assumeTrue("MailBox.jar missing - run mvn package first", Files.isRegularFile(mailboxJar));
        Assume.assumeTrue("ULM agent missing - build unifiedlicensemanager 1.1.0", Files.isRegularFile(agent));
        Assume.assumeTrue("ULM cli missing", Files.isRegularFile(cli));
        Assume.assumeTrue("ULM generator missing", Files.isRegularFile(generator));

        Path sandbox = Files.createTempDirectory("mailbox-ulm-it");
        Path home = sandbox.resolve("home");
        Path keys = sandbox.resolve("trusted-keys.properties");
        Path license = sandbox.resolve("MAILBOX.lic");
        Path dist = sandbox.resolve("dist");
        String passEnv = "ULM_MAILBOX_IT_PASS";

        try {
            Map<String, String> pass = new HashMap<String, String>();
            pass.put(passEnv, "mailbox integration passphrase 17");

            assertEquals(0, run(sandbox, generator, Arrays.asList("-Dulm.home=" + home), pass,
                    "init-key", "--passphrase-env", passEnv).code);
            assertEquals(0, run(sandbox, generator, Arrays.asList("-Dulm.home=" + home), pass,
                    "export-public-keys", "--out", keys.toString()).code);
            Run gen = run(sandbox, generator, Arrays.asList("-Dulm.home=" + home), pass,
                    "generate", "--type", "INTERNAL", "--customer", "MailBox QA", "--product", "MAILBOX",
                    "--out", license.toString(), "--passphrase-env", passEnv);
            assertEquals(gen.output, 0, gen.code);

            assertEquals(0, run(sandbox, cli, Collections.<String>emptyList(), Collections.<String, String>emptyMap(),
                    "integrate", "--jar", mailboxJar.toString(), "--product", "MAILBOX",
                    "--trusted-keys", keys.toString(), "--agent-jar", agent.toString(),
                    "--out", dist.toString(), "--force").code);

            Path bundled = dist.resolve("MailBox.jar");
            assertTrue(Files.isRegularFile(bundled));
            assertTrue(Files.isRegularFile(dist.resolve("run.bat")));

            Run missing = javaAgent(sandbox, dist, bundled, Arrays.asList(
                    "-Dulm.license.dir=" + sandbox.resolve("empty"),
                    "-Dulm.state.dir=" + sandbox.resolve("state1")));
            assertEquals(missing.output, 3, missing.code);
            assertTrue(missing.output.contains("MAILBOX requires a valid") || missing.output.contains("MISSING_LICENSE"));
            assertFalse("SMTP must not start without a license",
                    missing.output.toLowerCase().contains("smtp server started"));

            Run valid = javaAgent(sandbox, dist, bundled, Arrays.asList(
                    "-Dulm.license.file=" + license,
                    "-Dulm.state.dir=" + sandbox.resolve("state2"),
                    "-Djava.awt.headless=true"));
            assertFalse(valid.output, valid.code == 3);
            assertFalse(valid.output.contains("WRONG_PRODUCT"));
            assertFalse(valid.output.contains("INVALID_SIGNATURE"));

            Path mqlxLic = sandbox.resolve("MQLX.lic");
            assertEquals(0, run(sandbox, generator, Arrays.asList("-Dulm.home=" + home), pass,
                    "generate", "--type", "INTERNAL", "--customer", "X", "--product", "MQLX",
                    "--out", mqlxLic.toString(), "--passphrase-env", passEnv).code);
            Run wrong = javaAgent(sandbox, dist, bundled, Arrays.asList(
                    "-Dulm.license.file=" + mqlxLic,
                    "-Dulm.state.dir=" + sandbox.resolve("state3")));
            assertEquals(wrong.output, 3, wrong.code);
            assertTrue(wrong.output.contains("WRONG_PRODUCT") || wrong.output.contains("not for this application"));
        } finally {
            deleteRecursive(sandbox);
        }
    }

    private static Run javaAgent(Path sandbox, Path dist, Path jar, List<String> jvmArgs)
            throws IOException, InterruptedException {
        Path java = Paths.get(System.getProperty("java.home"), "bin",
                System.getProperty("os.name", "").toLowerCase().contains("win") ? "java.exe" : "java");
        List<String> cmd = new ArrayList<String>();
        cmd.add(java.toString());
        cmd.add("-javaagent:" + dist.resolve("ulm").resolve("ulm-agent.jar"));
        cmd.addAll(jvmArgs);
        cmd.add("-jar");
        cmd.add(jar.toString());
        cmd.add("-s");
        cmd.add("-p");
        cmd.add("2525");
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.environment().put("APPDATA", sandbox.resolve("appdata").toString());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        boolean finished = process.waitFor(15, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            String output = readFully(process.getInputStream());
            return new Run(0, output + "\n(timeout-after-start)");
        }
        return new Run(process.exitValue(), readFully(process.getInputStream()));
    }

    private static Run run(Path sandbox, Path jar, List<String> jvmArgs, Map<String, String> env, String... args)
            throws IOException, InterruptedException {
        Path java = Paths.get(System.getProperty("java.home"), "bin",
                System.getProperty("os.name", "").toLowerCase().contains("win") ? "java.exe" : "java");
        List<String> cmd = new ArrayList<String>();
        cmd.add(java.toString());
        cmd.addAll(jvmArgs);
        cmd.add("-jar");
        cmd.add(jar.toString());
        Collections.addAll(cmd, args);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.environment().put("APPDATA", sandbox.resolve("appdata").toString());
        pb.environment().putAll(env);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        assertTrue(process.waitFor(120, TimeUnit.SECONDS));
        return new Run(process.exitValue(), readFully(process.getInputStream()));
    }

    private static String readFully(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int n;
        while ((n = in.read(chunk)) >= 0) {
            buffer.write(chunk, 0, n);
        }
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    private static void deleteRecursive(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static final class Run {
        final int code;
        final String output;

        Run(int code, String output) {
            this.code = code;
            this.output = output;
        }
    }
}
