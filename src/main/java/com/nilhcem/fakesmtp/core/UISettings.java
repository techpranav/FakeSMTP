package com.nilhcem.fakesmtp.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User interface preferences (theme, notifications, reading options, window bounds).
 * <p>
 * Preferences are stored in {@code .mailbox-ui.properties} in the user profile directory,
 * separately from the {@link Configuration} file and from received emails.
 * Legacy {@code .fakeSMTP-ui.properties} is still read when the MailBox file is absent.
 * Missing or invalid values fall back to safe defaults.
 * </p>
 *
 * @since 2.1
 */
public enum UISettings {
	INSTANCE;

	public static final String THEME_LIGHT = "light";
	public static final String THEME_DARK = "dark";
	public static final String VIEW_HTML = "html";
	public static final String VIEW_TEXT = "text";

	public static final String THEME = "ui.theme";
	public static final String NOTIFICATIONS_ENABLED = "notifications.enabled";
	public static final String NOTIFICATIONS_DESKTOP = "notifications.desktop";
	public static final String NOTIFICATIONS_INAPP = "notifications.inapp";
	public static final String NOTIFICATIONS_TOAST_SECONDS = "notifications.toast.seconds";
	public static final String NOTIFICATIONS_PREVIEW = "notifications.preview";
	public static final String NOTIFICATIONS_SOUND = "notifications.sound";
	public static final String NOTIFICATIONS_EVERY_EMAIL = "notifications.every.email";
	public static final String NOTIFICATIONS_SHOW_SENDER = "notifications.show.sender";
	public static final String NOTIFICATIONS_SHOW_SUBJECT = "notifications.show.subject";
	public static final String NOTIFICATIONS_PRIVACY = "notifications.privacy";
	public static final String EXTERNAL_IMAGES = "reading.external.images";
	public static final String DEFAULT_VIEW = "reading.default.view";
	public static final String SORT_NEWEST_FIRST = "list.sort.newest.first";
	public static final String LOAD_SAVED_EMAILS = "mailbox.load.saved.emails";
	public static final String WINDOW_X = "window.x";
	public static final String WINDOW_Y = "window.y";
	public static final String WINDOW_WIDTH = "window.width";
	public static final String WINDOW_HEIGHT = "window.height";
	public static final String WINDOW_MAXIMIZED = "window.maximized";

	private static final String USER_SETTINGS_FILE = ".mailbox-ui.properties";
	private static final String LEGACY_USER_SETTINGS_FILE = ".fakeSMTP-ui.properties";
	private static final Logger LOGGER = LoggerFactory.getLogger(UISettings.class);

	private final Properties defaults = new Properties();
	private final Properties values;
	private File file = new File(System.getProperty("user.home"), USER_SETTINGS_FILE);

	UISettings() {
		defaults.setProperty(THEME, THEME_LIGHT);
		defaults.setProperty(NOTIFICATIONS_ENABLED, "true");
		defaults.setProperty(NOTIFICATIONS_DESKTOP, "true");
		defaults.setProperty(NOTIFICATIONS_INAPP, "true");
		defaults.setProperty(NOTIFICATIONS_TOAST_SECONDS, "8");
		defaults.setProperty(NOTIFICATIONS_PREVIEW, "true");
		defaults.setProperty(NOTIFICATIONS_SOUND, "false");
		defaults.setProperty(NOTIFICATIONS_EVERY_EMAIL, "true");
		defaults.setProperty(NOTIFICATIONS_SHOW_SENDER, "true");
		defaults.setProperty(NOTIFICATIONS_SHOW_SUBJECT, "true");
		defaults.setProperty(NOTIFICATIONS_PRIVACY, "false");
		defaults.setProperty(EXTERNAL_IMAGES, "false");
		defaults.setProperty(DEFAULT_VIEW, VIEW_HTML);
		defaults.setProperty(SORT_NEWEST_FIRST, "true");
		defaults.setProperty(LOAD_SAVED_EMAILS, "true");
		values = new Properties(defaults);
		resolveSettingsFile();
		load();
	}

	private void resolveSettingsFile() {
		File home = new File(System.getProperty("user.home"));
		File mailbox = new File(home, USER_SETTINGS_FILE);
		File legacy = new File(home, LEGACY_USER_SETTINGS_FILE);
		if (mailbox.isFile()) {
			file = mailbox;
		} else if (legacy.isFile()) {
			file = legacy;
		} else {
			file = mailbox;
		}
	}

	/**
	 * Uses another file to load and save settings (used by tests), and reloads values from it.
	 *
	 * @param settingsFile the properties file to use.
	 */
	public synchronized void useFile(File settingsFile) {
		file = settingsFile;
		values.clear();
		load();
	}

	public synchronized String get(String key) {
		return values.getProperty(key, "");
	}

	public synchronized void set(String key, String value) {
		if (value == null) {
			values.remove(key);
		} else {
			values.setProperty(key, value);
		}
	}

	public synchronized boolean getBoolean(String key) {
		String value = values.getProperty(key);
		if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
			return Boolean.parseBoolean(value);
		}
		return Boolean.parseBoolean(defaults.getProperty(key, "false"));
	}

	public synchronized void setBoolean(String key, boolean value) {
		values.setProperty(key, Boolean.toString(value));
	}

	/**
	 * Returns an integer value, or {@code defaultValue} if the value is missing or invalid.
	 *
	 * @param key the key.
	 * @param defaultValue the value to return if the key is missing or invalid.
	 * @return the integer value.
	 */
	public synchronized int getInt(String key, int defaultValue) {
		String value = values.getProperty(key);
		if (value == null) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException e) {
			LOGGER.warn("Ignoring invalid value for setting {}", key);
			return defaultValue;
		}
	}

	public synchronized void setInt(String key, int value) {
		values.setProperty(key, Integer.toString(value));
	}

	public boolean isDarkTheme() {
		return THEME_DARK.equals(get(THEME));
	}

	/**
	 * Writes the settings to the user settings file.
	 *
	 * @throws IOException if the file cannot be written.
	 */
	public synchronized void save() throws IOException {
		file = new File(System.getProperty("user.home"), USER_SETTINGS_FILE);
		FileOutputStream fos = new FileOutputStream(file);
		try {
			values.store(fos, "MailBox user interface settings");
		} finally {
			IOUtils.closeQuietly(fos);
		}
	}

	private void load() {
		if (!file.isFile() || !file.canRead()) {
			return;
		}
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			values.load(fis);
		} catch (IOException e) {
			LOGGER.warn("Could not read UI settings from {}, using defaults", file.getName(), e);
		} catch (IllegalArgumentException e) {
			LOGGER.warn("Malformed UI settings file {}, using defaults", file.getName(), e);
			values.clear();
		} finally {
			IOUtils.closeQuietly(fis);
		}
	}
}
