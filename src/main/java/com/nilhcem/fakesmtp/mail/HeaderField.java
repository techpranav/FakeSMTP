package com.nilhcem.fakesmtp.mail;

/**
 * One header line from a received message.
 *
 * @since 2.1
 */
public final class HeaderField {

	private final String name;
	private final String value;

	public HeaderField(String name, String value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}
}
