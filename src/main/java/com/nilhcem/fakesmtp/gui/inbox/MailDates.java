package com.nilhcem.fakesmtp.gui.inbox;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Formats the time FakeSMTP received a message.
 *
 * @since 2.1
 */
public final class MailDates {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a");

	private MailDates() {
		throw new UnsupportedOperationException();
	}

	public static String format(Date date) {
		if (date == null) {
			return "";
		}
		return FORMATTER.format(date.toInstant().atZone(ZoneId.systemDefault()));
	}
}
