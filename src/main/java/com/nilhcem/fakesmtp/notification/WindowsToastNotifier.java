package com.nilhcem.fakesmtp.notification;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shows a Windows Action Center toast via WinRT ({@code ToastNotificationManager}).
 * Isolated from the core mail model so other platforms can use a different provider.
 *
 * @since 2.1
 */
public final class WindowsToastNotifier {

	private static final Logger LOGGER = LoggerFactory.getLogger(WindowsToastNotifier.class);
	private static final String APP_ID = "MailBox.Desktop";

	private final Boolean windows = isWindows();
	private Boolean available;

	public boolean isSupported() {
		if (!windows.booleanValue()) {
			return false;
		}
		if (available != null) {
			return available.booleanValue();
		}
		available = Boolean.valueOf(probe());
		return available.booleanValue();
	}

	/**
	 * Displays a compact toast. Failures are logged and never thrown.
	 */
	public boolean show(String title, String line1, String line2, String line3) {
		if (!isSupported()) {
			return false;
		}
		File script = null;
		try {
			script = File.createTempFile("mailbox-toast-", ".ps1");
			String xml = buildToastXml(title, line1, line2, line3);
			try (BufferedWriter writer = new BufferedWriter(
					new OutputStreamWriter(Files.newOutputStream(script.toPath()), StandardCharsets.UTF_8))) {
				writer.write("$ErrorActionPreference = 'Stop'\r\n");
				writer.write("[Windows.UI.Notifications.ToastNotificationManager, Windows.UI.Notifications, ContentType = WindowsRuntime] | Out-Null\r\n");
				writer.write("[Windows.Data.Xml.Dom.XmlDocument, Windows.Data.Xml.Dom.XmlDocument, ContentType = WindowsRuntime] | Out-Null\r\n");
				writer.write("$xml = New-Object Windows.Data.Xml.Dom.XmlDocument\r\n");
				writer.write("$xml.LoadXml(@'\r\n");
				writer.write(xml);
				writer.write("\r\n'@)\r\n");
				writer.write("$toast = [Windows.UI.Notifications.ToastNotification]::new($xml)\r\n");
				writer.write("[Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier('");
				writer.write(APP_ID);
				writer.write("').Show($toast)\r\n");
			}
			ProcessBuilder builder = new ProcessBuilder(
					"powershell.exe",
					"-NoProfile",
					"-ExecutionPolicy", "Bypass",
					"-File", script.getAbsolutePath());
			builder.redirectErrorStream(true);
			Process process = builder.start();
			boolean finished = process.waitFor(8, TimeUnit.SECONDS);
			if (!finished) {
				process.destroyForcibly();
				LOGGER.warn("Windows toast process timed out");
				available = Boolean.FALSE;
				return false;
			}
			if (process.exitValue() != 0) {
				LOGGER.warn("Windows toast exited with code {}", Integer.valueOf(process.exitValue()));
				available = Boolean.FALSE;
				return false;
			}
			return true;
		} catch (Exception e) {
			LOGGER.warn("Could not show a Windows toast notification", e);
			available = Boolean.FALSE;
			return false;
		} finally {
			if (script != null && !script.delete()) {
				script.deleteOnExit();
			}
		}
	}

	private boolean probe() {
		try {
			ProcessBuilder builder = new ProcessBuilder(
					"powershell.exe",
					"-NoProfile",
					"-Command",
					"[Windows.UI.Notifications.ToastNotificationManager, Windows.UI.Notifications, ContentType = WindowsRuntime] | Out-Null; 'ok'");
			builder.redirectErrorStream(true);
			Process process = builder.start();
			boolean finished = process.waitFor(5, TimeUnit.SECONDS);
			return finished && process.exitValue() == 0;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			LOGGER.debug("WinRT toast probe interrupted", e);
			return false;
		} catch (IOException e) {
			LOGGER.debug("WinRT toast probe failed", e);
			return false;
		}
	}

	private static String buildToastXml(String title, String line1, String line2, String line3) {
		StringBuilder xml = new StringBuilder();
		xml.append("<toast><visual><binding template=\"ToastGeneric\">");
		xml.append("<text>").append(escapeXml(title)).append("</text>");
		if (!isBlank(line1)) {
			xml.append("<text>").append(escapeXml(line1)).append("</text>");
		}
		if (!isBlank(line2)) {
			xml.append("<text>").append(escapeXml(line2)).append("</text>");
		}
		if (!isBlank(line3)) {
			xml.append("<text>").append(escapeXml(line3)).append("</text>");
		}
		xml.append("</binding></visual></toast>");
		return xml.toString();
	}

	private static String escapeXml(String value) {
		if (value == null) {
			return "";
		}
		String trimmed = value.length() > 180 ? value.substring(0, 177) + "..." : value;
		return trimmed.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&apos;")
				.replace("\r", " ")
				.replace("\n", " ");
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	private static boolean isWindows() {
		String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		return os.contains("win");
	}
}
