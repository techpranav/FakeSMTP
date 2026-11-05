package com.nilhcem.fakesmtp.mail;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;

import ch.astorm.jotlmsg.OutlookMessage;
import ch.astorm.jotlmsg.OutlookMessageRecipient;

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
	 * Writes a genuine Outlook {@code .msg} (OLE compound file via jotlmsg / Apache POI).
	 * jotlmsg 1.x (Java 17-compatible) supports a plain-text primary body; the original HTML
	 * is attached as {@code message-body.html} so formatting is preserved for Outlook users.
	 */
	public static void saveAsMsg(InboxMessage message, File file) throws IOException {
		if (message == null || message.getParsed() == null) {
			throw new IOException("No message to export");
		}
		ParsedEmail parsed = message.getParsed();
		OutlookMessage outlook = new OutlookMessage();
		outlook.setSubject(Texts.isBlank(parsed.getSubject()) ? "(no subject)" : parsed.getSubject());
		if (parsed.getReceivedDate() != null) {
			outlook.setSentDate(parsed.getReceivedDate());
		}
		if (!Texts.isBlank(parsed.getPlainText())) {
			outlook.setPlainTextBody(parsed.getPlainText());
		} else if (!Texts.isBlank(parsed.getHtml())) {
			outlook.setPlainTextBody(stripTags(parsed.getHtml()));
		} else if (!Texts.isBlank(message.getRaw())) {
			outlook.setPlainTextBody(message.getRaw());
		}
		EmailAddress from = EmailAddresses.first(EmailAddresses.resolveFrom(parsed));
		if (from != null) {
			String email = EmailAddresses.emailOnly(from);
			String name = EmailAddresses.displayName(from);
			if (!Texts.isBlank(email)) {
				// jotlmsg stores this on both sender props; OutlookMsgEnhancer splits name/email after write.
				outlook.setFrom(!Texts.isBlank(name) && !name.equals(email) ? name + " <" + email + ">" : email);
			} else if (!Texts.isBlank(name)) {
				outlook.setFrom(name);
			}
		}
		addRecipients(outlook, OutlookMessageRecipient.Type.TO, EmailAddresses.resolveTo(parsed));
		addRecipients(outlook, OutlookMessageRecipient.Type.CC, EmailAddresses.resolveCc(parsed));
		addRecipients(outlook, OutlookMessageRecipient.Type.BCC, EmailAddresses.resolveBcc(parsed));
		if (parsed.getReplyTo() != null && !parsed.getReplyTo().isEmpty()) {
			java.util.ArrayList<String> reply = new java.util.ArrayList<String>();
			for (EmailAddress address : parsed.getReplyTo()) {
				if (address != null && !Texts.isBlank(address.getAddress())) {
					reply.add(address.getAddress());
				}
			}
			if (!reply.isEmpty()) {
				outlook.setReplyTo(reply);
			}
		}
		// Keep HTML attachment as a fallback; primary HTML body is written by OutlookMsgEnhancer.
		if (!Texts.isBlank(parsed.getHtml())) {
			byte[] htmlBytes = parsed.getHtml().getBytes(StandardCharsets.UTF_8);
			outlook.addAttachment("message-body.html", "text/html", new ByteArrayInputStream(htmlBytes));
		}
		for (MailAttachment attachment : parsed.getAttachments()) {
			addMsgAttachment(outlook, attachment);
		}
		for (Map.Entry<String, byte[]> inline : parsed.getInlineImages().entrySet()) {
			if (alreadyAttached(parsed.getAttachments(), inline.getKey())) {
				continue;
			}
			String cid = inline.getKey();
			String name = "inline-" + sanitizeCid(cid) + ".bin";
			outlook.addAttachment(name, "application/octet-stream", new ByteArrayInputStream(inline.getValue()));
		}
		outlook.writeTo(file);
		OutlookMsgEnhancer.enhance(file, parsed);
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

	private static void addMsgAttachment(OutlookMessage outlook, MailAttachment attachment) {
		if (attachment == null || attachment.getData() == null) {
			return;
		}
		String name = AttachmentFiles.safeName(attachment.getFileName());
		String type = attachment.getContentType();
		if (Texts.isBlank(type)) {
			type = "application/octet-stream";
		}
		outlook.addAttachment(name, type, new ByteArrayInputStream(attachment.getData()));
	}

	private static boolean alreadyAttached(List<MailAttachment> attachments, String cid) {
		if (attachments == null || Texts.isBlank(cid)) {
			return false;
		}
		String needle = stripCidBrackets(cid);
		for (MailAttachment attachment : attachments) {
			if (attachment != null && needle.equalsIgnoreCase(stripCidBrackets(attachment.getContentId()))) {
				return true;
			}
		}
		return false;
	}

	private static String stripCidBrackets(String cid) {
		if (cid == null) {
			return "";
		}
		String value = cid.trim();
		if (value.startsWith("<") && value.endsWith(">") && value.length() > 2) {
			return value.substring(1, value.length() - 1);
		}
		return value;
	}

	private static String sanitizeCid(String cid) {
		return stripCidBrackets(cid).replaceAll("[^A-Za-z0-9._-]+", "_");
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

	private static String stripTags(String html) {
		return html.replaceAll("(?is)<script.*?>.*?</script>", " ")
				.replaceAll("(?is)<style.*?>.*?</style>", " ")
				.replaceAll("(?is)<[^>]+>", " ")
				.replaceAll("\\s+", " ")
				.trim();
	}
}
