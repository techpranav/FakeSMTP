package com.nilhcem.fakesmtp.model;

import java.util.Date;
import java.util.UUID;

import com.nilhcem.fakesmtp.mail.ParsedEmail;

/**
 * One row in the inbox: the stored message plus its parsed view and read flag.
 *
 * @since 2.1
 */
public final class InboxMessage {

	private final String id;
	private final EmailModel email;
	private final ParsedEmail parsed;
	private boolean read;

	public InboxMessage(EmailModel email, ParsedEmail parsed, boolean read) {
		this.id = UUID.randomUUID().toString();
		this.email = email;
		this.parsed = parsed;
		this.read = read;
	}

	public String getId() {
		return id;
	}

	public EmailModel getEmail() {
		return email;
	}

	public ParsedEmail getParsed() {
		return parsed;
	}

	public boolean isRead() {
		return read;
	}

	public void setRead(boolean read) {
		this.read = read;
	}

	public Date getReceivedDate() {
		if (parsed != null && parsed.getReceivedDate() != null) {
			return parsed.getReceivedDate();
		}
		if (email != null) {
			return email.getReceivedDate();
		}
		return null;
	}

	public String getFilePath() {
		if (parsed != null && parsed.getFilePath() != null && !parsed.getFilePath().isEmpty()) {
			return parsed.getFilePath();
		}
		if (email != null) {
			return email.getFilePath();
		}
		return null;
	}

	public String getRaw() {
		if (email != null && email.getEmailStr() != null) {
			return email.getEmailStr();
		}
		if (parsed != null) {
			return parsed.getRaw();
		}
		return "";
	}
}
