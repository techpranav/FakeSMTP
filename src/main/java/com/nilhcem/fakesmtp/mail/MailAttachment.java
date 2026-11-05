package com.nilhcem.fakesmtp.mail;

/**
 * A file extracted from a MIME part.
 *
 * @since 2.1
 */
public final class MailAttachment {

	private final String fileName;
	private final String contentType;
	private final String contentId;
	private final byte[] data;
	private final boolean inline;

	public MailAttachment(String fileName, String contentType, String contentId, byte[] data, boolean inline) {
		this.fileName = fileName;
		this.contentType = contentType;
		this.contentId = contentId;
		this.data = data;
		this.inline = inline;
	}

	public String getFileName() {
		return fileName;
	}

	public String getContentType() {
		return contentType;
	}

	public String getContentId() {
		return contentId;
	}

	public byte[] getData() {
		return data;
	}

	public int getSize() {
		if (data == null) {
			return 0;
		}
		return data.length;
	}

	public boolean isInline() {
		return inline;
	}
}
