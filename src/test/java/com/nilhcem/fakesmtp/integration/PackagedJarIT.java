package com.nilhcem.fakesmtp.integration;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

/**
 * Starts the packaged executable jar and checks that it still receives mail without a GUI.
 */
public class PackagedJarIT {

	private static final int PORT = 2536;

	@Test
	public void executableJarReceivesAnEmailInBackground() throws Exception {
		String jarPath = System.getProperty("mailbox.jar");
		if (jarPath == null || jarPath.isEmpty()) {
			jarPath = System.getProperty("fakesmtp.jar");
		}
		File jar = new File(jarPath);
		assertTrue("Packaged jar was not built: " + jar.getAbsolutePath(), jar.isFile());
		File output = Files.createTempDirectory("mailbox-jar").toFile();
		String javaBin = new File(System.getProperty("java.home"), "bin/java").getAbsolutePath();
		if (new File(javaBin + ".exe").isFile()) {
			javaBin = javaBin + ".exe";
		}
		Process process = new ProcessBuilder(javaBin, "-jar", jar.getAbsolutePath(),
				"-s", "-b", "-p", Integer.toString(PORT), "-o", output.getAbsolutePath())
				.redirectErrorStream(true)
				.start();
		Thread drain = new Thread(() -> {
			try {
				byte[] buffer = new byte[4096];
				while (process.getInputStream().read(buffer) >= 0) {
					// Discard process output so the pipe cannot fill.
				}
			} catch (Exception ignored) {
				// The process is stopped by the test.
			}
		});
		drain.setDaemon(true);
		drain.start();
		try {
			waitForPort();
			sendMessage();
			assertTrue(waitForSavedEmail(output));
		} finally {
			process.destroy();
			process.waitFor(5, TimeUnit.SECONDS);
			process.destroyForcibly();
		}
	}

	private static void waitForPort() throws Exception {
		long deadline = System.currentTimeMillis() + 20000;
		Exception last = null;
		while (System.currentTimeMillis() < deadline) {
			try (Socket socket = new Socket()) {
				socket.connect(new InetSocketAddress("127.0.0.1", PORT), 500);
				return;
			} catch (Exception e) {
				last = e;
				Thread.sleep(200);
			}
		}
		throw new IllegalStateException("Packaged MailBox did not open port " + PORT, last);
	}

	private static void sendMessage() throws Exception {
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress("127.0.0.1", PORT), 2000);
			socket.setSoTimeout(2000);
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
			reader.readLine();
			writer.print("EHLO localhost\r\n");
			writer.flush();
			readReply(reader);
			writer.print("MAIL FROM:<jar@example.com>\r\n");
			writer.flush();
			reader.readLine();
			writer.print("RCPT TO:<dev@example.com>\r\n");
			writer.flush();
			reader.readLine();
			writer.print("DATA\r\n");
			writer.flush();
			reader.readLine();
			writer.print("Subject: Packaged jar\r\n");
			writer.print("\r\nHello from the packaged jar.\r\n.\r\n");
			writer.flush();
			reader.readLine();
			writer.print("QUIT\r\n");
			writer.flush();
		}
	}

	private static void readReply(BufferedReader reader) throws Exception {
		String line = reader.readLine();
		while (line != null && line.length() > 3 && line.charAt(3) == '-') {
			line = reader.readLine();
		}
	}

	private static boolean waitForSavedEmail(File output) throws Exception {
		long deadline = System.currentTimeMillis() + 10000;
		while (System.currentTimeMillis() < deadline) {
			File[] files = output.listFiles();
			if (files != null) {
				for (File file : files) {
					if (file.getName().endsWith(".eml") && file.length() > 0) {
						return true;
					}
				}
			}
			Thread.sleep(200);
		}
		return false;
	}
}
