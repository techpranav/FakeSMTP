package com.nilhcem.fakesmtp.gui.inbox;

import com.nilhcem.fakesmtp.core.Texts;

import java.awt.Component;
import java.awt.Desktop;
import java.net.URI;
import java.util.Locale;

import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opens a link from an email only when it is a normal web or mail address.
 *
 * @since 2.1
 */
public final class SafeLinks {

	private static final Logger LOGGER = LoggerFactory.getLogger(SafeLinks.class);

	private SafeLinks() {
		throw new UnsupportedOperationException();
	}

	public static void open(Component parent, String uri) {
		URI parsed = parse(uri);
		if (parsed == null || !allowed(parsed.getScheme())) {
			JOptionPane.showMessageDialog(parent, InboxText.format("link.blocked", String.valueOf(uri)),
					InboxText.get("link.blocked.title"), JOptionPane.WARNING_MESSAGE);
			return;
		}
		if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
			JOptionPane.showMessageDialog(parent, InboxText.format("link.blocked", parsed.toString()),
					InboxText.get("link.blocked.title"), JOptionPane.WARNING_MESSAGE);
			return;
		}
		try {
			Desktop.getDesktop().browse(parsed);
		} catch (Exception e) {
			LOGGER.warn("Could not open a link from an email", e);
			JOptionPane.showMessageDialog(parent, InboxText.format("link.blocked", parsed.toString()),
					InboxText.get("link.blocked.title"), JOptionPane.WARNING_MESSAGE);
		}
	}

	private static URI parse(String uri) {
		if (Texts.isBlank(uri)) {
			return null;
		}
		try {
			return new URI(uri.trim());
		} catch (Exception e) {
			LOGGER.debug("Ignoring a link that is not a URI", e);
			return null;
		}
	}

	private static boolean allowed(String scheme) {
		if (scheme == null) {
			return false;
		}
		String value = scheme.toLowerCase(Locale.ROOT);
		return "http".equals(value) || "https".equals(value) || "mailto".equals(value);
	}
}
