package com.nilhcem.fakesmtp.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;

/**
 * Contains and returns some project-specific configuration variables.
 *
 * @author Nilhcem
 * @since 1.0
 */
public enum Configuration {
	INSTANCE;

	private static final String CONFIG_FILE = "/configuration.properties";
	/** Preferred MailBox user settings file. */
	private static final String USER_CONFIG_FILE = ".mailbox.properties";
	/** Legacy FakeSMTP settings file — still loaded for backward compatibility. */
	private static final String LEGACY_USER_CONFIG_FILE = ".fakeSMTP.properties";
	private final Properties defaults = new Properties();
	private final Properties config = new Properties();

	/**
	 * Opens the "{@code configuration.properties}" file and maps data.
	 */
	Configuration() {
		InputStream in = getClass().getResourceAsStream(CONFIG_FILE);
		try {
			defaults.load(in);
			in.close();
			config.putAll(defaults);
			loadFromUserProfile();
			restoreBranding();
		} catch (IOException e) {
			LoggerFactory.getLogger(Configuration.class).error("", e);
		}
	}

	/**
	 * Returns the value of a specific entry from the "{@code configuration.properties}" file.
	 *
	 * @param key a string representing the key from a key/value couple.
	 * @return the value of the key, or an empty string if the key was not found.
	 */
	public String get(String key) {
		if (config.containsKey(key)) {
			return config.getProperty(key);
		}
		return "";
	}

	/**
	 * Sets the value of a specific entry.
	 *
	 * @param key a string representing the key from a key/value couple.
	 * @param value the value of the key.
	 */
	public void set(String key, String value) {
		config.setProperty(key, value);
	}

	/**
	 * Saves configuration to file.
	 *
	 * @param file file to save configuration.
	 * @throws IOException
	 */
	public void saveToFile(File file) throws IOException {
		Properties user = new Properties();
		// Local array: enum INSTANCE initializes before other static fields.
		String[] userKeys = { "smtp.default.port", "emails.default.dir", "application.tray.use" };
		for (String key : userKeys) {
			String value = config.getProperty(key);
			if (value != null) {
				user.setProperty(key, value);
			}
		}
		FileOutputStream fos = new FileOutputStream(file);
		try {
			user.store(fos, "MailBox user settings");
		} finally {
			IOUtils.closeQuietly(fos);
		}
	}

	/**
	 * Saves configuration to {@code .mailbox.properties} in the user profile directory.
	 * Calls {@link Configuration#saveToFile(java.io.File)}.
	 *
	 * @throws IOException
	 */
	public void saveToUserProfile() throws IOException {
		saveToFile(new File(System.getProperty("user.home"), USER_CONFIG_FILE));
	}

	/**
	 * Loads configuration from file.
	 *
	 * @param file file to load configuration.
	 * @return INSTANCE.
	 * @throws IOException
	 */
	public Configuration loadFromFile(File file) throws IOException {
		if (file.exists() && file.canRead()) {
			FileInputStream fis = new FileInputStream(file);
			try {
				config.load(fis);
			} finally {
				IOUtils.closeQuietly(fis);
			}
			restoreBranding();
		}
		return INSTANCE;
	}

	/**
	 * Loads configuration from the user profile.
	 * <p>
	 * Loads the legacy {@code .fakeSMTP.properties} first (if present), then
	 * {@code .mailbox.properties} so newer MailBox settings override without losing old installs.
	 * Branding keys are always restored from the packaged defaults afterward.
	 * </p>
	 *
	 * @return INSTANCE.
	 * @throws IOException
	 */
	public Configuration loadFromUserProfile() throws IOException {
		String home = System.getProperty("user.home");
		loadFromFile(new File(home, LEGACY_USER_CONFIG_FILE));
		loadFromFile(new File(home, USER_CONFIG_FILE));
		restoreBranding();
		return INSTANCE;
	}

	private void restoreBranding() {
		// Local array: enum INSTANCE initializes before other static fields.
		String[] brandingKeys = {
			"application.name",
			"application.title",
			"application.tagline",
			"application.description",
			"application.product.code",
			"application.icon.path"
		};
		for (String key : brandingKeys) {
			String value = defaults.getProperty(key);
			if (value != null) {
				config.setProperty(key, value);
			}
		}
	}
}
