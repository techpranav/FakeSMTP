package com.nilhcem.fakesmtp.mail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A received message broken into headers, bodies, and attachments.
 * <p>
 * The original text is kept unchanged so the raw view can show it.
 * </p>
 *
 * @since 2.1
 */
public final class ParsedEmail {

	private final String raw;
	private final String envelopeFrom;
	private final String envelopeTo;
	private final Date receivedDate;
	private final String filePath;
	private final boolean readable;
	private final String subject;
	private final List<EmailAddress> from;
	private final List<EmailAddress> to;
	private final List<EmailAddress> cc;
	private final List<EmailAddress> bcc;
	private final List<EmailAddress> replyTo;
	private final String messageId;
	private final String contentType;
	private final String transferEncoding;
	private final List<HeaderField> headers;
	private final String plainText;
	private final String html;
	private final String preview;
	private final List<MailAttachment> attachments;
	private final Map<String, byte[]> inlineImages;
	private String preparedHtml;
	private int blockedImages;

	private ParsedEmail(Builder builder) {
		raw = builder.raw;
		envelopeFrom = builder.envelopeFrom;
		envelopeTo = builder.envelopeTo;
		receivedDate = builder.receivedDate;
		filePath = builder.filePath;
		readable = builder.readable;
		subject = builder.subject;
		from = unmodifiable(builder.from);
		to = unmodifiable(builder.to);
		cc = unmodifiable(builder.cc);
		bcc = unmodifiable(builder.bcc);
		replyTo = unmodifiable(builder.replyTo);
		messageId = builder.messageId;
		contentType = builder.contentType;
		transferEncoding = builder.transferEncoding;
		headers = Collections.unmodifiableList(new ArrayList<HeaderField>(builder.headers));
		plainText = builder.plainText;
		html = builder.html;
		preview = builder.preview;
		attachments = Collections.unmodifiableList(new ArrayList<MailAttachment>(builder.attachments));
		inlineImages = Collections.unmodifiableMap(new LinkedHashMap<String, byte[]>(builder.inlineImages));
		preparedHtml = builder.preparedHtml;
		blockedImages = builder.blockedImages;
	}

	public static Builder builder() {
		return new Builder();
	}

	public String getRaw() {
		return raw;
	}

	public String getEnvelopeFrom() {
		return envelopeFrom;
	}

	public String getEnvelopeTo() {
		return envelopeTo;
	}

	public Date getReceivedDate() {
		return receivedDate;
	}

	public String getFilePath() {
		return filePath;
	}

	public boolean isReadable() {
		return readable;
	}

	public String getSubject() {
		return subject;
	}

	public List<EmailAddress> getFrom() {
		return from;
	}

	public List<EmailAddress> getTo() {
		return to;
	}

	public List<EmailAddress> getCc() {
		return cc;
	}

	public List<EmailAddress> getBcc() {
		return bcc;
	}

	public List<EmailAddress> getReplyTo() {
		return replyTo;
	}

	public String getMessageId() {
		return messageId;
	}

	public String getContentType() {
		return contentType;
	}

	public String getTransferEncoding() {
		return transferEncoding;
	}

	public List<HeaderField> getHeaders() {
		return headers;
	}

	public String getPlainText() {
		return plainText;
	}

	public String getHtml() {
		return html;
	}

	public String getPreview() {
		return preview;
	}

	public List<MailAttachment> getAttachments() {
		return attachments;
	}

	public boolean hasAttachments() {
		return !attachments.isEmpty();
	}

	public Map<String, byte[]> getInlineImages() {
		return inlineImages;
	}

	public String getPreparedHtml() {
		return preparedHtml;
	}

	public int getBlockedImages() {
		return blockedImages;
	}

	/**
	 * Replaces the rendered HTML after a change such as loading remote images.
	 *
	 * @param prepared the new document.
	 */
	public void applyPrepared(PreparedHtml prepared) {
		if (prepared == null) {
			preparedHtml = null;
			blockedImages = 0;
			return;
		}
		preparedHtml = prepared.getXhtml();
		blockedImages = prepared.getBlockedImages();
	}

	public String senderLine() {
		String formatted = join(from);
		if (!formatted.isEmpty()) {
			return formatted;
		}
		if (envelopeFrom != null) {
			return envelopeFrom;
		}
		return "";
	}

	public String recipientLine() {
		String formatted = join(to);
		if (!formatted.isEmpty()) {
			return formatted;
		}
		if (envelopeTo != null) {
			return envelopeTo;
		}
		return "";
	}

	/**
	 * Text searched from the inbox: sender, recipients, and subject.
	 *
	 * @return a single lower-case-ready string of those fields.
	 */
	public String searchText() {
		StringBuilder builder = new StringBuilder();
		append(builder, senderLine());
		append(builder, recipientLine());
		append(builder, join(cc));
		append(builder, join(bcc));
		append(builder, join(replyTo));
		append(builder, envelopeFrom);
		append(builder, envelopeTo);
		append(builder, subject);
		append(builder, preview);
		return builder.toString();
	}

	private static void append(StringBuilder builder, String value) {
		if (value == null || value.isEmpty()) {
			return;
		}
		if (builder.length() > 0) {
			builder.append('\n');
		}
		builder.append(value);
	}

	public static String join(List<EmailAddress> addresses) {
		StringBuilder builder = new StringBuilder();
		if (addresses == null) {
			return "";
		}
		for (EmailAddress address : addresses) {
			String formatted = address.format();
			if (formatted.isEmpty()) {
				continue;
			}
			if (builder.length() > 0) {
				builder.append(", ");
			}
			builder.append(formatted);
		}
		return builder.toString();
	}

	private static List<EmailAddress> unmodifiable(List<EmailAddress> values) {
		return Collections.unmodifiableList(new ArrayList<EmailAddress>(values));
	}

	/**
	 * Collects the fields of a parsed message before it is frozen.
	 */
	public static final class Builder {
		private String raw = "";
		private String envelopeFrom = "";
		private String envelopeTo = "";
		private Date receivedDate;
		private String filePath;
		private boolean readable;
		private String subject = "";
		private List<EmailAddress> from = new ArrayList<EmailAddress>();
		private List<EmailAddress> to = new ArrayList<EmailAddress>();
		private List<EmailAddress> cc = new ArrayList<EmailAddress>();
		private List<EmailAddress> bcc = new ArrayList<EmailAddress>();
		private List<EmailAddress> replyTo = new ArrayList<EmailAddress>();
		private String messageId = "";
		private String contentType = "";
		private String transferEncoding = "";
		private List<HeaderField> headers = new ArrayList<HeaderField>();
		private String plainText = "";
		private String html;
		private String preview = "";
		private List<MailAttachment> attachments = new ArrayList<MailAttachment>();
		private Map<String, byte[]> inlineImages = new LinkedHashMap<String, byte[]>();
		private String preparedHtml;
		private int blockedImages;

		public Builder raw(String value) {
			raw = value;
			return this;
		}

		public Builder envelopeFrom(String value) {
			envelopeFrom = value;
			return this;
		}

		public Builder envelopeTo(String value) {
			envelopeTo = value;
			return this;
		}

		public Builder receivedDate(Date value) {
			receivedDate = value;
			return this;
		}

		public Builder filePath(String value) {
			filePath = value;
			return this;
		}

		public Builder readable(boolean value) {
			readable = value;
			return this;
		}

		public Builder subject(String value) {
			subject = value;
			return this;
		}

		public Builder from(List<EmailAddress> value) {
			from = value;
			return this;
		}

		public Builder to(List<EmailAddress> value) {
			to = value;
			return this;
		}

		public Builder cc(List<EmailAddress> value) {
			cc = value;
			return this;
		}

		public Builder bcc(List<EmailAddress> value) {
			bcc = value;
			return this;
		}

		public Builder replyTo(List<EmailAddress> value) {
			replyTo = value;
			return this;
		}

		public Builder messageId(String value) {
			messageId = value;
			return this;
		}

		public Builder contentType(String value) {
			contentType = value;
			return this;
		}

		public Builder transferEncoding(String value) {
			transferEncoding = value;
			return this;
		}

		public Builder headers(List<HeaderField> value) {
			headers = value;
			return this;
		}

		public Builder plainText(String value) {
			plainText = value;
			return this;
		}

		public Builder html(String value) {
			html = value;
			return this;
		}

		public Builder preview(String value) {
			preview = value;
			return this;
		}

		public Builder attachments(List<MailAttachment> value) {
			attachments = value;
			return this;
		}

		public Builder inlineImages(Map<String, byte[]> value) {
			inlineImages = value;
			return this;
		}

		public Builder preparedHtml(String value) {
			preparedHtml = value;
			return this;
		}

		public Builder blockedImages(int value) {
			blockedImages = value;
			return this;
		}

		public ParsedEmail build() {
			return new ParsedEmail(this);
		}
	}
}
