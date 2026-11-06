package com.nilhcem.fakesmtp.mail;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.imageio.ImageIO;

import com.nilhcem.fakesmtp.core.Texts;
import com.nilhcem.fakesmtp.model.InboxMessage;

/**
 * Saves a received message as Outlook {@code .msg}, {@code .eml}, or a full-page PNG
 * that includes metadata plus the complete HTML body.
 *
 * @since 2.1
 */
public final class EmailExport {

	private EmailExport() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Writes an Outlook-openable {@code .msg} with From/To/CC display fields, HTML body,
	 * and attachments (via {@link OutlookMsgWriter}).
	 */
	public static void saveAsMsg(InboxMessage message, File file) throws IOException {
		OutlookMsgWriter.write(message, file);
	}

	/**
	 * Captures subject, recipients, date and the complete HTML body into one PNG.
	 * Does not depend on the reading-pane scroll position.
	 */
	public static void saveScreenshot(InboxMessage message, File file) throws IOException {
		if (message == null || message.getParsed() == null) {
			throw new IOException("No message to export");
		}
		try {
			BufferedImage image = EmailScreenshot.capture(message.getParsed());
			ImageIO.write(image, "png", file);
		} catch (IllegalArgumentException e) {
			throw new IOException(e.getMessage() + " Try saving as EML/MSG instead.", e);
		}
	}

	public static void saveAsEml(InboxMessage message, File file) throws IOException {
		if (message == null) {
			throw new IOException("No message to export");
		}
		String raw = message.getRaw();
		if (Texts.isBlank(raw)) {
			throw new IOException("Original MIME source is not available for this message");
		}
		byte[] data = raw.getBytes(StandardCharsets.UTF_8);
		try (FileOutputStream output = new FileOutputStream(file)) {
			output.write(data);
		}
	}

	/**
	 * Builds a default export base name: {@code Subject_yyyyMMdd_HHmmss}.
	 */
	public static String suggestedFileBase(InboxMessage message, String prefix) {
		String subject = "email";
		Date when = new Date();
		if (message != null && message.getParsed() != null) {
			if (!Texts.isBlank(message.getParsed().getSubject())) {
				subject = message.getParsed().getSubject();
			}
			if (message.getParsed().getReceivedDate() != null) {
				when = message.getParsed().getReceivedDate();
			}
		}
		String cleaned = sanitizeFileToken(subject);
		String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(when);
		if (Texts.isBlank(prefix)) {
			return cleaned + "_" + stamp;
		}
		return sanitizeFileToken(prefix) + "_" + cleaned + "_" + stamp;
	}

	public static String sanitizeFileToken(String value) {
		if (value == null) {
			return "email";
		}
		String cleaned = value.replaceAll("[\\\\/:*?\"<>|\\r\\n\\t]+", "_").trim();
		cleaned = cleaned.replaceAll("\\s+", "_");
		if (cleaned.length() > 60) {
			cleaned = cleaned.substring(0, 60).trim();
		}
		while (cleaned.endsWith("_") || cleaned.endsWith(".")) {
			cleaned = cleaned.substring(0, cleaned.length() - 1);
		}
		if (Texts.isBlank(cleaned)) {
			return "email";
		}
		return cleaned;
	}
}
