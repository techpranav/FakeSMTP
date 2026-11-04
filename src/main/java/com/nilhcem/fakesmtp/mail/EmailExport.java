package com.nilhcem.fakesmtp.mail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import ch.astorm.jotlmsg.OutlookMessage;
import ch.astorm.jotlmsg.OutlookMessageRecipient;

import com.nilhcem.fakesmtp.core.Texts;
import com.nilhcem.fakesmtp.model.InboxMessage;

/**
 * Saves a received message as Outlook {@code .msg}, {@code .eml}, or a full-page PNG.
 *
 * @since 2.1
 */
public final class EmailExport {

	private static final int SCREENSHOT_WIDTH = 900;
	private static final int MAX_SCREENSHOT_HEIGHT = 20000;

	private EmailExport() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Writes an Outlook {@code .msg} file with subject, recipients, plain text, and attachments.
	 * The original HTML is also attached as {@code message-body.html} so formatting is preserved
	 * when the jotlmsg Java 8 API cannot embed HTML as the primary body.
	 */
	public static void saveAsMsg(InboxMessage message, File file) throws IOException {
		if (message == null || message.getParsed() == null) {
			throw new IOException("No message to export");
		}
		ParsedEmail parsed = message.getParsed();
		OutlookMessage outlook = new OutlookMessage();
		outlook.setSubject(Texts.isBlank(parsed.getSubject()) ? "(no subject)" : parsed.getSubject());
		if (!Texts.isBlank(parsed.getPlainText())) {
			outlook.setPlainTextBody(parsed.getPlainText());
		} else if (!Texts.isBlank(parsed.getHtml())) {
			outlook.setPlainTextBody(stripTags(parsed.getHtml()));
		} else {
			outlook.setPlainTextBody(message.getRaw());
		}
		EmailAddress from = first(parsed.getFrom());
		if (from != null && !Texts.isBlank(from.getAddress())) {
			if (!Texts.isBlank(from.getPersonal())) {
				outlook.setFrom(from.getPersonal() + " <" + from.getAddress() + ">");
			} else {
				outlook.setFrom(from.getAddress());
			}
		}
		addRecipients(outlook, OutlookMessageRecipient.Type.TO, parsed.getTo());
		addRecipients(outlook, OutlookMessageRecipient.Type.CC, parsed.getCc());
		addRecipients(outlook, OutlookMessageRecipient.Type.BCC, parsed.getBcc());
		if (!Texts.isBlank(parsed.getHtml())) {
			byte[] htmlBytes = parsed.getHtml().getBytes(StandardCharsets.UTF_8);
			outlook.addAttachment("message-body.html", "text/html", new ByteArrayInputStream(htmlBytes));
		}
		for (MailAttachment attachment : parsed.getAttachments()) {
			String name = AttachmentFiles.safeName(attachment.getFileName());
			String type = attachment.getContentType();
			if (Texts.isBlank(type)) {
				type = "application/octet-stream";
			}
			outlook.addAttachment(name, type, new ByteArrayInputStream(attachment.getData()));
		}
		outlook.writeTo(file);
	}

	public static void saveScreenshot(InboxMessage message, File file) throws IOException {
		if (message == null || message.getParsed() == null) {
			throw new IOException("No message to export");
		}
		ParsedEmail parsed = message.getParsed();
		if (Texts.isBlank(parsed.getPreparedHtml()) && Texts.isBlank(parsed.getHtml())) {
			throw new IOException("This message has no HTML body to capture");
		}
		if (Texts.isBlank(parsed.getPreparedHtml()) && !Texts.isBlank(parsed.getHtml())) {
			parsed.applyPrepared(HtmlPreprocessor.prepare(parsed.getHtml(), false));
		}
		java.awt.image.BufferedImage image = HtmlRenderer.renderFull(parsed, SCREENSHOT_WIDTH, MAX_SCREENSHOT_HEIGHT);
		javax.imageio.ImageIO.write(image, "png", file);
	}

	public static void saveAsEml(InboxMessage message, File file) throws IOException {
		if (message == null) {
			throw new IOException("No message to export");
		}
		byte[] data = message.getRaw().getBytes(StandardCharsets.UTF_8);
		try (FileOutputStream output = new FileOutputStream(file)) {
			output.write(data);
		}
	}

	private static void addRecipients(OutlookMessage outlook, OutlookMessageRecipient.Type type,
			List<EmailAddress> addresses) {
		if (addresses == null) {
			return;
		}
		for (EmailAddress address : addresses) {
			if (address == null || Texts.isBlank(address.getAddress())) {
				continue;
			}
			outlook.addRecipient(type, address.getAddress(), address.getPersonal());
		}
	}

	private static EmailAddress first(List<EmailAddress> addresses) {
		if (addresses == null || addresses.isEmpty()) {
			return null;
		}
		return addresses.get(0);
	}

	private static String stripTags(String html) {
		return html.replaceAll("(?is)<script.*?>.*?</script>", " ")
				.replaceAll("(?is)<style.*?>.*?</style>", " ")
				.replaceAll("(?is)<[^>]+>", " ")
				.replaceAll("\\s+", " ")
				.trim();
	}
}
