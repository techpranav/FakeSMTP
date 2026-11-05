package com.nilhcem.fakesmtp.gui.inbox;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * English text for the inbox window.
 * <p>
 * Kept in its own bundle so the translated application bundles stay untouched.
 * </p>
 *
 * @since 2.1
 */
public final class InboxText {

	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("i18n/inbox");

	private InboxText() {
		throw new UnsupportedOperationException();
	}

	public static String get(String key) {
		try {
			return BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return key;
		}
	}

	public static String format(String key, Object... arguments) {
		return MessageFormat.format(get(key), arguments);
	}
}
