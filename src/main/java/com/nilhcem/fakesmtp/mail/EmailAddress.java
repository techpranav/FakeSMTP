package com.nilhcem.fakesmtp.mail;

/**
 * A display name and mailbox address taken from a message header.
 *
 * @since 2.1
 */
public final class EmailAddress {

	private final String personal;
	private final String address;

	public EmailAddress(String personal, String address) {
		this.personal = personal;
		this.address = address;
	}

	public String getPersonal() {
		return personal;
	}

	public String getAddress() {
		return address;
	}

	/**
	 * @return a single line such as {@code Ada Lovelace <ada@example.com>}.
	 */
	public String format() {
		boolean hasPersonal = personal != null && !personal.isBlank();
		boolean hasAddress = address != null && !address.isBlank();
		if (hasPersonal && hasAddress) {
			return personal + " <" + address + ">";
		}
		if (hasPersonal) {
			return personal;
		}
		if (hasAddress) {
			return address;
		}
		return "";
	}
}
