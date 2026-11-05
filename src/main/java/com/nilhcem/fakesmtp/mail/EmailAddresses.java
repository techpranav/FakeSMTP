package com.nilhcem.fakesmtp.mail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.nilhcem.fakesmtp.core.Texts;

/**
 * Resolves and formats From/To/CC/BCC for export, screenshot, and Outlook MSG.
 *
 * @since 2.1
 */
public final class EmailAddresses {

	private EmailAddresses() {
		throw new UnsupportedOperationException();
	}

	public static List<EmailAddress> resolveFrom(ParsedEmail parsed) {
		if (parsed == null) {
			return Collections.emptyList();
		}
		if (hasAny(parsed.getFrom())) {
			return parsed.getFrom();
		}
		return singletonEnvelope(parsed.getEnvelopeFrom());
	}

	public static List<EmailAddress> resolveTo(ParsedEmail parsed) {
		if (parsed == null) {
			return Collections.emptyList();
		}
		if (hasAny(parsed.getTo())) {
			return parsed.getTo();
		}
		return singletonEnvelope(parsed.getEnvelopeTo());
	}

	public static List<EmailAddress> resolveCc(ParsedEmail parsed) {
		if (parsed == null || parsed.getCc() == null) {
			return Collections.emptyList();
		}
		return parsed.getCc();
	}

	public static List<EmailAddress> resolveBcc(ParsedEmail parsed) {
		if (parsed == null || parsed.getBcc() == null) {
			return Collections.emptyList();
		}
		return parsed.getBcc();
	}

	public static String formatList(List<EmailAddress> addresses) {
		if (addresses == null || addresses.isEmpty()) {
			return "";
		}
		StringBuilder out = new StringBuilder();
		for (EmailAddress address : addresses) {
			if (address == null) {
				continue;
			}
			String line = address.format();
			if (Texts.isBlank(line)) {
				continue;
			}
			if (out.length() > 0) {
				out.append("; ");
			}
			out.append(line);
		}
		return out.toString();
	}

	public static String displayNames(List<EmailAddress> addresses) {
		if (addresses == null || addresses.isEmpty()) {
			return "";
		}
		StringBuilder out = new StringBuilder();
		for (EmailAddress address : addresses) {
			if (address == null) {
				continue;
			}
			String name = displayName(address);
			if (Texts.isBlank(name)) {
				continue;
			}
			if (out.length() > 0) {
				out.append("; ");
			}
			out.append(name);
		}
		return out.toString();
	}

	public static String displayName(EmailAddress address) {
		if (address == null) {
			return "";
		}
		if (!Texts.isBlank(address.getPersonal())) {
			return address.getPersonal().trim();
		}
		if (!Texts.isBlank(address.getAddress())) {
			return address.getAddress().trim();
		}
		return "";
	}

	public static String emailOnly(EmailAddress address) {
		if (address == null || Texts.isBlank(address.getAddress())) {
			return "";
		}
		return address.getAddress().trim();
	}

	public static EmailAddress first(List<EmailAddress> addresses) {
		if (addresses == null || addresses.isEmpty()) {
			return null;
		}
		for (EmailAddress address : addresses) {
			if (address != null && (!Texts.isBlank(address.getAddress()) || !Texts.isBlank(address.getPersonal()))) {
				return address;
			}
		}
		return null;
	}

	private static boolean hasAny(List<EmailAddress> addresses) {
		return first(addresses) != null;
	}

	private static List<EmailAddress> singletonEnvelope(String envelope) {
		if (Texts.isBlank(envelope)) {
			return Collections.emptyList();
		}
		String value = envelope.trim();
		int lt = value.indexOf('<');
		int gt = value.indexOf('>');
		if (lt >= 0 && gt > lt) {
			String personal = value.substring(0, lt).trim();
			String email = value.substring(lt + 1, gt).trim();
			List<EmailAddress> list = new ArrayList<EmailAddress>(1);
			list.add(new EmailAddress(Texts.isBlank(personal) ? null : personal, email));
			return list;
		}
		return Collections.singletonList(new EmailAddress(null, value));
	}
}
