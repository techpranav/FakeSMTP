package com.nilhcem.fakesmtp.mail;

import com.nilhcem.fakesmtp.core.Texts;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nilhcem.fakesmtp.model.EmailModel;

/**
 * Reads a raw RFC 822 message into a {@link ParsedEmail}.
 * <p>
 * A message that cannot be parsed is still returned, with the original text preserved,
 * so one bad message cannot drop the rest of the inbox.
 * </p>
 *
 * @since 2.1
 */
public final class EmailParser {

	private static final Logger LOGGER = LoggerFactory.getLogger(EmailParser.class);
	private static final int PREVIEW_LIMIT = 140;

	private EmailParser() {
		throw new UnsupportedOperationException();
	}

	public static ParsedEmail parse(EmailModel model, boolean loadRemoteImages) {
		if (model == null) {
			return unreadable("", "", "", new Date(), null);
		}
		return parse(model.getEmailStr(), model.getFrom(), model.getTo(), model.getReceivedDate(),
				model.getFilePath(), loadRemoteImages);
	}

	/**
	 * Parses the raw message text.
	 *
	 * @param raw the original message, unchanged.
	 * @param envelopeFrom the SMTP envelope sender, used when the header is missing.
	 * @param envelopeTo the SMTP envelope recipient, used when the header is missing.
	 * @param received when MailBox stored the message.
	 * @param filePath the saved {@code .eml} file, or {@code null} in memory mode.
	 * @param loadRemoteImages whether remote images may be downloaded while preparing HTML.
	 * @return the parsed message, or an unreadable placeholder.
	 */
	public static ParsedEmail parse(String raw, String envelopeFrom, String envelopeTo, Date received,
			String filePath, boolean loadRemoteImages) {
		String source = "";
		if (raw != null) {
			source = raw;
		}
		try {
			String normalized = source.replace("\r\n", "\n").replace('\r', '\n').replace("\n", "\r\n");
			Session session = Session.getInstance(sessionProperties());
			MimeMessage message = new MimeMessage(session, new ByteArrayInputStream(normalized.getBytes(StandardCharsets.UTF_8)));
			return readMessage(message, source, envelopeFrom, envelopeTo, received, filePath, loadRemoteImages);
		} catch (Exception e) {
			LOGGER.warn("Could not parse a received email", e);
			return unreadable(source, envelopeFrom, envelopeTo, received, filePath);
		}
	}

	private static Properties sessionProperties() {
		Properties properties = new Properties();
		properties.setProperty("mail.mime.address.strict", "false");
		return properties;
	}

	private static ParsedEmail readMessage(MimeMessage message, String raw, String envelopeFrom, String envelopeTo,
			Date received, String filePath, boolean loadRemoteImages) throws Exception {
		Collected collected = new Collected();
		walk(message, collected);
		String subject = safeSubject(message);
		String plain = collected.plain;
		if (plain == null) {
			plain = "";
		}
		String preview = preview(plain, collected.html);
		PreparedHtml prepared = null;
		if (collected.html != null && !collected.html.isEmpty()) {
			prepared = HtmlPreprocessor.prepare(collected.html, loadRemoteImages);
		}
		List<EmailAddress> from = addresses(message.getFrom());
		List<EmailAddress> to = recipients(message, Message.RecipientType.TO);
		if (from.isEmpty() && !Texts.isBlank(envelopeFrom)) {
			from = envelopeAddresses(envelopeFrom);
		}
		if (to.isEmpty() && !Texts.isBlank(envelopeTo)) {
			to = envelopeAddresses(envelopeTo);
		}
		ParsedEmail.Builder builder = ParsedEmail.builder()
				.raw(raw)
				.envelopeFrom(empty(envelopeFrom))
				.envelopeTo(empty(envelopeTo))
				.receivedDate(received)
				.filePath(filePath)
				.readable(true)
				.subject(subject)
				.from(from)
				.to(to)
				.cc(recipients(message, Message.RecipientType.CC))
				.bcc(recipients(message, Message.RecipientType.BCC))
				.replyTo(addresses(message.getReplyTo()))
				.messageId(empty(message.getMessageID()))
				.contentType(empty(message.getContentType()))
				.transferEncoding(empty(message.getEncoding()))
				.headers(headerFields(message))
				.plainText(plain)
				.html(collected.html)
				.preview(preview)
				.attachments(collected.attachments)
				.inlineImages(collected.inlineImages);
		if (prepared != null) {
			builder.preparedHtml(prepared.getXhtml()).blockedImages(prepared.getBlockedImages());
		}
		return builder.build();
	}

	private static void walk(Part part, Collected collected) throws Exception {
		if (part.isMimeType("multipart/*")) {
			Multipart multipart = (Multipart) part.getContent();
			for (int i = 0; i < multipart.getCount(); i++) {
				walk(multipart.getBodyPart(i), collected);
			}
			return;
		}
		boolean attachment = Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition());
		if (!attachment && part.isMimeType("text/plain") && collected.plain == null) {
			collected.plain = text(part);
			return;
		}
		if (!attachment && part.isMimeType("text/html") && collected.html == null) {
			collected.html = text(part);
			return;
		}
		if (part.isMimeType("message/rfc822")) {
			Object content = part.getContent();
			if (content instanceof Part) {
				walk((Part) content, collected);
				return;
			}
		}
		addFile(part, collected);
	}

	private static void addFile(Part part, Collected collected) throws Exception {
		byte[] data = readBytes(part.getInputStream());
		String contentId = contentId(part);
		String fileName = fileName(part);
		String contentType = empty(part.getContentType());
		boolean markedAttachment = Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition());
		boolean imagePart = isImagePart(contentType, data);
		// CID images stay available for HTML; office docs / files still appear as attachments
		// even when disposition=inline so Excel tables and similar embeds are not dropped.
		if (contentId != null && imagePart) {
			collected.inlineImages.put(contentId, data);
		}
		if (markedAttachment || !imagePart || contentId == null) {
			boolean inlineOnly = contentId != null && imagePart && !markedAttachment;
			if (!inlineOnly) {
				collected.attachments.add(new MailAttachment(fileName, contentType, contentId, data,
						contentId != null && !markedAttachment));
			}
		} else if (contentId != null) {
			// Image with CID and disposition=inline — already stored in inlineImages.
		}
	}

	private static boolean isImagePart(String contentType, byte[] data) {
		String type = contentType == null ? "" : contentType.toLowerCase(java.util.Locale.ROOT);
		if (type.startsWith("image/")) {
			return true;
		}
		String sniffed = ImageBytes.mimeType(data);
		return sniffed != null && sniffed.startsWith("image/");
	}

	private static String text(Part part) throws Exception {
		Object content = part.getContent();
		if (content instanceof String) {
			return (String) content;
		}
		Charset charset = StandardCharsets.UTF_8;
		try {
			String name = new ContentType(part.getContentType()).getParameter("charset");
			if (!Texts.isBlank(name)) {
				charset = Charset.forName(name);
			}
		} catch (Exception e) {
			LOGGER.debug("Using UTF-8 for a text part without a usable charset", e);
		}
		return new String(readBytes(part.getInputStream()), charset);
	}

	private static byte[] readBytes(InputStream input) throws Exception {
		try (InputStream in = input) {
			return org.apache.commons.io.IOUtils.toByteArray(in);
		}
	}

	private static String contentId(Part part) throws MessagingException {
		String[] values = part.getHeader("Content-ID");
		if (values == null || values.length == 0 || values[0] == null) {
			return null;
		}
		String value = values[0].trim();
		if (value.startsWith("<") && value.endsWith(">") && value.length() > 2) {
			return value.substring(1, value.length() - 1);
		}
		return value;
	}

	private static String fileName(Part part) {
		try {
			String name = part.getFileName();
			if (Texts.isBlank(name)) {
				return "attachment";
			}
			return MimeUtility.decodeText(name);
		} catch (Exception e) {
			LOGGER.debug("Could not read an attachment file name", e);
			return "attachment";
		}
	}

	private static String safeSubject(MimeMessage message) {
		try {
			String subject = message.getSubject();
			if (subject == null) {
				return "";
			}
			return subject;
		} catch (MessagingException e) {
			LOGGER.debug("Could not read the subject", e);
			return "";
		}
	}

	private static List<EmailAddress> recipients(MimeMessage message, Message.RecipientType type) {
		try {
			return addresses(message.getRecipients(type));
		} catch (MessagingException e) {
			LOGGER.debug("Could not read recipients", e);
			return new ArrayList<EmailAddress>();
		}
	}

	private static List<EmailAddress> addresses(Address[] values) {
		List<EmailAddress> result = new ArrayList<EmailAddress>();
		if (values == null) {
			return result;
		}
		for (Address value : values) {
			if (value instanceof InternetAddress) {
				InternetAddress internet = (InternetAddress) value;
				result.add(new EmailAddress(internet.getPersonal(), internet.getAddress()));
			} else if (value != null) {
				result.add(new EmailAddress(null, value.toString()));
			}
		}
		return result;
	}

	private static List<EmailAddress> envelopeAddresses(String envelope) {
		List<EmailAddress> result = new ArrayList<EmailAddress>();
		if (Texts.isBlank(envelope)) {
			return result;
		}
		try {
			InternetAddress[] parsed = InternetAddress.parse(envelope, false);
			return addresses(parsed);
		} catch (Exception e) {
			result.add(new EmailAddress(null, envelope.trim()));
			return result;
		}
	}

	private static List<HeaderField> headerFields(MimeMessage message) throws MessagingException {
		List<HeaderField> fields = new ArrayList<HeaderField>();
		Enumeration<?> all = message.getAllHeaders();
		while (all.hasMoreElements()) {
			Header header = (Header) all.nextElement();
			String value = header.getValue();
			try {
				value = MimeUtility.decodeText(MimeUtility.unfold(value));
			} catch (Exception e) {
				LOGGER.debug("Leaving a header value undecoded", e);
			}
			fields.add(new HeaderField(header.getName(), value));
		}
		return fields;
	}

	private static String preview(String plain, String html) {
		String text = plain;
		if (Texts.isBlank(text)) {
			if (html == null) {
				text = "";
			} else {
				text = Jsoup.parse(html).text();
			}
		}
		text = text.replaceAll("\\s+", " ").trim();
		if (text.length() <= PREVIEW_LIMIT) {
			return text;
		}
		return text.substring(0, PREVIEW_LIMIT).trim() + "...";
	}

	private static ParsedEmail unreadable(String raw, String envelopeFrom, String envelopeTo, Date received, String filePath) {
		String preview = raw;
		if (preview == null) {
			preview = "";
		}
		preview = preview.replaceAll("\\s+", " ").trim();
		if (preview.length() > PREVIEW_LIMIT) {
			preview = preview.substring(0, PREVIEW_LIMIT).trim() + "...";
		}
		return ParsedEmail.builder()
				.raw(raw)
				.envelopeFrom(empty(envelopeFrom))
				.envelopeTo(empty(envelopeTo))
				.receivedDate(received)
				.filePath(filePath)
				.readable(false)
				.plainText(raw == null ? "" : raw)
				.preview(preview)
				.build();
	}

	private static String empty(String value) {
		if (value == null) {
			return "";
		}
		return value;
	}

	private static final class Collected {
		private String plain;
		private String html;
		private final List<MailAttachment> attachments = new ArrayList<MailAttachment>();
		private final Map<String, byte[]> inlineImages = new LinkedHashMap<String, byte[]>();
	}
}
