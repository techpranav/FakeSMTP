package com.nilhcem.fakesmtp.mail;

import static ch.astorm.jotlmsg.io.PropertiesChunk.FLAG_READABLE;
import static ch.astorm.jotlmsg.io.PropertiesChunk.FLAG_WRITEABLE;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.hsmf.datatypes.AttachmentChunks;
import org.apache.poi.hsmf.datatypes.MAPIProperty;
import org.apache.poi.hsmf.datatypes.NameIdChunks;
import org.apache.poi.hsmf.datatypes.PropertyValue;
import org.apache.poi.hsmf.datatypes.RecipientChunks;
import org.apache.poi.hsmf.datatypes.Types;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.util.StringUtil;

import ch.astorm.jotlmsg.io.MessagePropertiesChunk;
import ch.astorm.jotlmsg.io.PropertiesChunk;
import ch.astorm.jotlmsg.io.StoragePropertiesChunk;

import com.nilhcem.fakesmtp.core.Texts;
import com.nilhcem.fakesmtp.model.InboxMessage;

/**
 * Writes Outlook-openable {@code .msg} files with From/To/CC display fields and HTML body.
 * Built on jotlmsg's property chunk writers (same structure Outlook accepts), without post-hoc
 * stream patching that corrupts the compound file.
 *
 * @since 2.1
 */
public final class OutlookMsgWriter {

	private static final long FLAGS = FLAG_READABLE | FLAG_WRITEABLE;
	/** FILETIME epoch offset used by POI / jotlmsg. */
	private static final long FILETIME_OFFSET = 1000L * 60L * 60L * 24L * (365L * 369L + 89L);
	/** STORE_UNICODE_OK | STORE_HTML_OK | STORE_RTF_OK */
	private static final int STORE_SUPPORT = 0x00040000 | 0x00010000 | 0x00000800;
	/**
	 * Outlook stores {@code PR_HTML}/{@code PidTagHtml} as binary UTF-8 (0x1013/0102),
	 * not as a Unicode string stream (001F).
	 */
	private static final MAPIProperty HTML_BINARY =
			MAPIProperty.createCustom(0x1013, Types.BINARY, "HTML");
	/** PidTagInternetCodepage = UTF-8 */
	private static final int INTERNET_CPID_UTF8 = 65001;

	private OutlookMsgWriter() {
		throw new UnsupportedOperationException();
	}

	public static void write(InboxMessage message, File file) throws IOException {
		if (message == null || message.getParsed() == null) {
			throw new IOException("No message to export");
		}
		ParsedEmail parsed = message.getParsed();
		List<EmailAddress> fromList = EmailAddresses.resolveFrom(parsed);
		List<EmailAddress> toList = EmailAddresses.resolveTo(parsed);
		List<EmailAddress> ccList = EmailAddresses.resolveCc(parsed);
		List<EmailAddress> bccList = EmailAddresses.resolveBcc(parsed);
		EmailAddress sender = EmailAddresses.first(fromList);
		String senderName = sender == null ? "" : EmailAddresses.displayName(sender);
		String senderEmail = sender == null ? "" : EmailAddresses.emailOnly(sender);
		if (Texts.isBlank(senderName) && !Texts.isBlank(senderEmail)) {
			senderName = senderEmail;
		}
		String subject = Texts.isBlank(parsed.getSubject()) ? "(no subject)" : parsed.getSubject();
		String plain = plainBody(parsed, message.getRaw());
		String html = parsed.getHtml();
		Date sent = parsed.getReceivedDate() != null ? parsed.getReceivedDate() : new Date();
		String displayTo = EmailAddresses.displayNames(toList);
		String displayCc = EmailAddresses.displayNames(ccList);
		String displayBcc = EmailAddresses.displayNames(bccList);

		List<AttachmentSpec> attachments = collectAttachments(parsed);

		POIFSFileSystem fs = new POIFSFileSystem();
		try {
			DirectoryEntry nameid = fs.createDirectory(NameIdChunks.NAME);
			nameid.createDocument(PropertiesChunk.PREFIX + "00020102", new ByteArrayInputStream(new byte[0]));
			nameid.createDocument(PropertiesChunk.PREFIX + "00030102", new ByteArrayInputStream(new byte[0]));
			nameid.createDocument(PropertiesChunk.PREFIX + "00040102", new ByteArrayInputStream(new byte[0]));

			MessagePropertiesChunk top = new MessagePropertiesChunk();
			top.setAttachmentCount(attachments.size());
			top.setRecipientCount(countRecipients(toList, ccList, bccList));
			top.setNextAttachmentId(attachments.size());
			top.setNextRecipientId(countRecipients(toList, ccList, bccList));

			top.setProperty(new PropertyValue(MAPIProperty.STORE_SUPPORT_MASK, FLAGS,
					ByteBuffer.allocate(4).putInt(STORE_SUPPORT).array()));
			top.setProperty(unicode(MAPIProperty.MESSAGE_CLASS, "IPM.Note"));
			top.setProperty(new PropertyValue(MAPIProperty.HASATTACH, FLAGS,
					attachments.isEmpty() ? new byte[] { 0 } : new byte[] { 1 }));
			top.setProperty(new PropertyValue(MAPIProperty.MESSAGE_FLAGS, FLAGS,
					ByteBuffer.allocate(4).putInt(2).array()));
			top.setProperty(new PropertyValue(MAPIProperty.CLIENT_SUBMIT_TIME, FLAGS, dateToBytes(sent)));
			top.setProperty(new PropertyValue(MAPIProperty.INTERNET_CPID, FLAGS,
					ByteBuffer.allocate(4).putInt(INTERNET_CPID_UTF8).array()));
			top.setProperty(unicode(MAPIProperty.TRANSPORT_MESSAGE_HEADERS, transportHeaders(parsed, sent,
					senderName, senderEmail, toList, ccList, bccList, subject)));
			top.setProperty(unicode(MAPIProperty.SUBJECT, subject));
			if (!Texts.isBlank(plain)) {
				top.setProperty(unicode(MAPIProperty.BODY, plain));
			}
			if (!Texts.isBlank(html)) {
				// Binary PR_HTML + fromhtml RTF — required for Outlook to open as HTML (not Plain Text).
				top.setProperty(new PropertyValue(HTML_BINARY, FLAGS, MsgHtmlBody.htmlBinary(html)));
				top.setProperty(new PropertyValue(MAPIProperty.RTF_COMPRESSED, FLAGS,
						MsgHtmlBody.rtfCompressedFromHtml(html)));
			}
			if (!Texts.isBlank(senderName)) {
				top.setProperty(unicode(MAPIProperty.SENDER_NAME, senderName));
				top.setProperty(unicode(MAPIProperty.SENT_REPRESENTING_NAME, senderName));
			}
			if (!Texts.isBlank(senderEmail)) {
				top.setProperty(unicode(MAPIProperty.SENDER_EMAIL_ADDRESS, senderEmail));
				top.setProperty(unicode(MAPIProperty.SENT_REPRESENTING_EMAIL_ADDRESS, senderEmail));
			}
			top.setProperty(unicode(MAPIProperty.SENDER_ADDRTYPE, "SMTP"));
			top.setProperty(unicode(MAPIProperty.SENT_REPRESENTING_ADDRTYPE, "SMTP"));
			top.setProperty(unicode(MAPIProperty.DISPLAY_TO, displayTo));
			top.setProperty(unicode(MAPIProperty.DISPLAY_CC, displayCc));
			top.setProperty(unicode(MAPIProperty.DISPLAY_BCC, displayBcc));
			top.writeTo(fs.getRoot());

			int recipientCounter = 0;
			recipientCounter = writeRecipients(fs, toList, 1, recipientCounter);
			recipientCounter = writeRecipients(fs, ccList, 2, recipientCounter);
			writeRecipients(fs, bccList, 3, recipientCounter);

			int attachmentCounter = 0;
			for (AttachmentSpec attachment : attachments) {
				writeAttachment(fs, attachment, attachmentCounter++);
			}

			try (FileOutputStream output = new FileOutputStream(file)) {
				fs.writeFilesystem(output);
			}
		} finally {
			fs.close();
		}
	}

	private static int writeRecipients(POIFSFileSystem fs, List<EmailAddress> addresses, int recipientType,
			int startIndex) throws IOException {
		int index = startIndex;
		if (addresses == null) {
			return index;
		}
		for (EmailAddress address : addresses) {
			if (address == null) {
				continue;
			}
			String email = EmailAddresses.emailOnly(address);
			String name = EmailAddresses.displayName(address);
			if (Texts.isBlank(email) && Texts.isBlank(name)) {
				continue;
			}
			if (Texts.isBlank(name)) {
				name = email;
			}
			if (index >= 2048) {
				throw new IOException("Too many recipients for MSG export");
			}
			StoragePropertiesChunk recip = new StoragePropertiesChunk();
			recip.setProperty(new PropertyValue(MAPIProperty.OBJECT_TYPE, FLAGS,
					ByteBuffer.allocate(4).putInt(6).array()));
			recip.setProperty(new PropertyValue(MAPIProperty.DISPLAY_TYPE, FLAGS,
					ByteBuffer.allocate(4).putInt(0).array()));
			recip.setProperty(new PropertyValue(MAPIProperty.RECIPIENT_TYPE, FLAGS,
					ByteBuffer.allocate(4).putInt(recipientType).array()));
			recip.setProperty(new PropertyValue(MAPIProperty.ROWID, FLAGS,
					ByteBuffer.allocate(4).putInt(index).array()));
			recip.setProperty(unicode(MAPIProperty.DISPLAY_NAME, name));
			recip.setProperty(unicode(MAPIProperty.RECIPIENT_DISPLAY_NAME, name));
			if (!Texts.isBlank(email)) {
				recip.setProperty(unicode(MAPIProperty.EMAIL_ADDRESS, email));
				recip.setProperty(unicode(MAPIProperty.SMTP_ADDRESS, email));
			}
			recip.setProperty(unicode(MAPIProperty.ADDRTYPE, "SMTP"));
			String rid = String.format("%08x", index);
			DirectoryEntry folder = fs.createDirectory(RecipientChunks.PREFIX + rid);
			recip.writeTo(folder);
			index++;
		}
		return index;
	}

	private static void writeAttachment(POIFSFileSystem fs, AttachmentSpec attachment, int index)
			throws IOException {
		StoragePropertiesChunk attach = new StoragePropertiesChunk();
		attach.setProperty(new PropertyValue(MAPIProperty.OBJECT_TYPE, FLAGS,
				ByteBuffer.allocate(4).putInt(7).array()));
		attach.setProperty(unicode(MAPIProperty.ATTACH_FILENAME, attachment.name));
		attach.setProperty(unicode(MAPIProperty.ATTACH_LONG_FILENAME, attachment.name));
		if (!Texts.isBlank(attachment.mimeType)) {
			attach.setProperty(unicode(MAPIProperty.ATTACH_MIME_TAG, attachment.mimeType));
		}
		attach.setProperty(new PropertyValue(MAPIProperty.ATTACH_NUM, FLAGS,
				ByteBuffer.allocate(4).putInt(index).array()));
		attach.setProperty(new PropertyValue(MAPIProperty.ATTACH_METHOD, FLAGS,
				ByteBuffer.allocate(4).putInt(1).array()));
		attach.setProperty(new PropertyValue(MAPIProperty.ATTACH_DATA, FLAGS, attachment.data));
		String rid = String.format("%08x", index);
		DirectoryEntry folder = fs.createDirectory(AttachmentChunks.PREFIX + rid);
		attach.writeTo(folder);
	}

	private static List<AttachmentSpec> collectAttachments(ParsedEmail parsed) {
		List<AttachmentSpec> list = new ArrayList<AttachmentSpec>();
		// Do not attach message-body.html — HTML is embedded as the message body for Outlook.
		for (MailAttachment attachment : parsed.getAttachments()) {
			if (attachment == null || attachment.getData() == null) {
				continue;
			}
			String name = AttachmentFiles.safeName(attachment.getFileName());
			String type = Texts.isBlank(attachment.getContentType())
					? "application/octet-stream" : attachment.getContentType();
			list.add(new AttachmentSpec(name, type, attachment.getData()));
		}
		for (Map.Entry<String, byte[]> inline : parsed.getInlineImages().entrySet()) {
			if (inline.getValue() == null) {
				continue;
			}
			boolean already = false;
			for (MailAttachment attachment : parsed.getAttachments()) {
				if (attachment != null && inline.getKey() != null
						&& inline.getKey().equalsIgnoreCase(stripCid(attachment.getContentId()))) {
					already = true;
					break;
				}
			}
			if (already) {
				continue;
			}
			String name = "inline-" + sanitizeCid(inline.getKey()) + ".bin";
			list.add(new AttachmentSpec(name, "application/octet-stream", inline.getValue()));
		}
		return list;
	}

	private static int countRecipients(List<EmailAddress> to, List<EmailAddress> cc, List<EmailAddress> bcc) {
		return countUsable(to) + countUsable(cc) + countUsable(bcc);
	}

	private static int countUsable(List<EmailAddress> addresses) {
		int count = 0;
		if (addresses == null) {
			return 0;
		}
		for (EmailAddress address : addresses) {
			if (address != null && (!Texts.isBlank(address.getAddress()) || !Texts.isBlank(address.getPersonal()))) {
				count++;
			}
		}
		return count;
	}

	private static String plainBody(ParsedEmail parsed, String raw) {
		if (!Texts.isBlank(parsed.getPlainText())) {
			return parsed.getPlainText();
		}
		if (!Texts.isBlank(parsed.getHtml())) {
			return parsed.getHtml()
					.replaceAll("(?is)<script.*?>.*?</script>", " ")
					.replaceAll("(?is)<style.*?>.*?</style>", " ")
					.replaceAll("(?is)<[^>]+>", " ")
					.replaceAll("\\s+", " ")
					.trim();
		}
		return Texts.isBlank(raw) ? "" : raw;
	}

	private static String transportHeaders(ParsedEmail parsed, Date sent, String senderName, String senderEmail,
			List<EmailAddress> to, List<EmailAddress> cc, List<EmailAddress> bcc, String subject) {
		SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US);
		StringBuilder headers = new StringBuilder();
		headers.append("Date: ").append(format.format(sent)).append("\r\n");
		if (!Texts.isBlank(senderEmail) || !Texts.isBlank(senderName)) {
			headers.append("From: ");
			if (!Texts.isBlank(senderName) && !Texts.isBlank(senderEmail)) {
				headers.append(senderName).append(" <").append(senderEmail).append(">");
			} else if (!Texts.isBlank(senderEmail)) {
				headers.append(senderEmail);
			} else {
				headers.append(senderName);
			}
			headers.append("\r\n");
		}
		appendHeaderList(headers, "To", to);
		appendHeaderList(headers, "Cc", cc);
		appendHeaderList(headers, "Bcc", bcc);
		headers.append("Subject: ").append(subject).append("\r\n");
		headers.append("MIME-Version: 1.0\r\n");
		if (!Texts.isBlank(parsed.getMessageId())) {
			headers.append("Message-ID: ").append(parsed.getMessageId()).append("\r\n");
		}
		return headers.toString();
	}

	private static void appendHeaderList(StringBuilder headers, String label, List<EmailAddress> addresses) {
		String value = EmailAddresses.formatList(addresses);
		if (!Texts.isBlank(value)) {
			headers.append(label).append(": ").append(value).append("\r\n");
		}
	}

	private static PropertyValue unicode(MAPIProperty property, String value) {
		return new PropertyValue(property, FLAGS, StringUtil.getToUnicodeLE(value == null ? "" : value));
	}

	private static byte[] dateToBytes(Date date) {
		return ByteBuffer.allocate(8).putLong((date.getTime() + FILETIME_OFFSET) * 10 * 1000).array();
	}

	private static String stripCid(String cid) {
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
		return stripCid(cid).replaceAll("[^A-Za-z0-9._-]+", "_");
	}

	private static final class AttachmentSpec {
		private final String name;
		private final String mimeType;
		private final byte[] data;

		private AttachmentSpec(String name, String mimeType, byte[] data) {
			this.name = name;
			this.mimeType = mimeType;
			this.data = data;
		}
	}
}
