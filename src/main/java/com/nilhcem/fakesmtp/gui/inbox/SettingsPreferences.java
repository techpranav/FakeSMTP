package com.nilhcem.fakesmtp.gui.inbox;

import com.nilhcem.fakesmtp.core.UISettings;
import com.nilhcem.fakesmtp.notification.NotificationPreferences;

/**
 * Reads notification preferences from {@link UISettings} each time a message arrives.
 *
 * @since 2.1
 */
public final class SettingsPreferences implements NotificationPreferences {

	private final WindowRef window;

	public SettingsPreferences(WindowRef window) {
		this.window = window;
	}

	@Override
	public boolean notificationsEnabled() {
		return UISettings.INSTANCE.getBoolean(UISettings.NOTIFICATIONS_ENABLED);
	}

	@Override
	public boolean previewEnabled() {
		return UISettings.INSTANCE.getBoolean(UISettings.NOTIFICATIONS_PREVIEW);
	}

	@Override
	public boolean soundEnabled() {
		return UISettings.INSTANCE.getBoolean(UISettings.NOTIFICATIONS_SOUND);
	}

	@Override
	public boolean notifyWhenFocused() {
		return UISettings.INSTANCE.getBoolean(UISettings.NOTIFICATIONS_EVERY_EMAIL);
	}

	@Override
	public boolean applicationFocused() {
		return window.isFocused();
	}

	@Override
	public boolean showSender() {
		return UISettings.INSTANCE.getBoolean(UISettings.NOTIFICATIONS_SHOW_SENDER);
	}

	@Override
	public boolean showSubject() {
		return UISettings.INSTANCE.getBoolean(UISettings.NOTIFICATIONS_SHOW_SUBJECT);
	}

	@Override
	public boolean privacyMode() {
		return UISettings.INSTANCE.getBoolean(UISettings.NOTIFICATIONS_PRIVACY);
	}
}
