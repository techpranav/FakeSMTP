package com.nilhcem.fakesmtp.gui.inbox;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nilhcem.fakesmtp.core.Texts;
import com.nilhcem.fakesmtp.mail.EmailAddress;

/**
 * Validates mailbox addresses and opens a {@code mailto:} URI in the system mail client.
 *
 * @since 2.1
 */
public final class EmailAddressActions {

	private static final Logger LOGGER = LoggerFactory.getLogger(EmailAddressActions.class);
	private static final Pattern ADDRESS = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

	private EmailAddressActions() {
		throw new UnsupportedOperationException();
	}

	public static boolean isValidAddress(String address) {
		if (Texts.isBlank(address)) {
			return false;
		}
		String trimmed = address.trim();
		if (trimmed.length() > 254 || trimmed.contains("..") || trimmed.startsWith(".") || trimmed.endsWith(".")) {
			return false;
		}
		return ADDRESS.matcher(trimmed).matches();
	}

	public static void openMailto(Component parent, EmailAddress address) {
		if (address == null || !isValidAddress(address.getAddress())) {
			JOptionPane.showMessageDialog(parent, InboxText.get("address.invalid"),
					InboxText.get("address.invalid.title"), JOptionPane.WARNING_MESSAGE);
			return;
		}
		openMailto(parent, address.getAddress());
	}

	public static void openMailto(Component parent, String address) {
		if (!isValidAddress(address)) {
			JOptionPane.showMessageDialog(parent, InboxText.get("address.invalid"),
					InboxText.get("address.invalid.title"), JOptionPane.WARNING_MESSAGE);
			return;
		}
		if (!Desktop.isDesktopSupported()) {
			JOptionPane.showMessageDialog(parent, InboxText.get("address.noClient"),
					InboxText.get("address.invalid.title"), JOptionPane.WARNING_MESSAGE);
			return;
		}
		Desktop desktop = Desktop.getDesktop();
		String encoded = encodeMailbox(address.trim());
		URI uri = URI.create("mailto:" + encoded);
		try {
			if (desktop.isSupported(Desktop.Action.MAIL)) {
				desktop.mail(uri);
				return;
			}
			if (desktop.isSupported(Desktop.Action.BROWSE)) {
				desktop.browse(uri);
				return;
			}
			JOptionPane.showMessageDialog(parent, InboxText.get("address.noClient"),
					InboxText.get("address.invalid.title"), JOptionPane.WARNING_MESSAGE);
		} catch (Exception e) {
			LOGGER.warn("Could not open mailto:{}", address, e);
			JOptionPane.showMessageDialog(parent, InboxText.get("address.noClient"),
					InboxText.get("address.invalid.title"), JOptionPane.WARNING_MESSAGE);
		}
	}

	public static void copyAddress(EmailAddress address) {
		if (address == null || Texts.isBlank(address.getAddress())) {
			return;
		}
		copyText(address.getAddress().trim());
	}

	public static void copyDisplayName(EmailAddress address) {
		if (address == null || Texts.isBlank(address.getPersonal())) {
			return;
		}
		copyText(address.getPersonal().trim());
	}

	public static void copyText(String value) {
		if (Texts.isBlank(value)) {
			return;
		}
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(value), null);
	}

	private static String encodeMailbox(String address) {
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < address.length(); i++) {
			char ch = address.charAt(i);
			if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9')
					|| ch == '@' || ch == '.' || ch == '_' || ch == '-' || ch == '+') {
				out.append(ch);
			} else {
				out.append(URLEncoder.encode(String.valueOf(ch), StandardCharsets.UTF_8).toLowerCase(Locale.ROOT));
			}
		}
		return out.toString();
	}
}
