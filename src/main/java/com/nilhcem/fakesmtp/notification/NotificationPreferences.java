package com.nilhcem.fakesmtp.notification;

/**
 * The notification choices the user can change from settings.
 *
 * @since 2.1
 */
public interface NotificationPreferences {

	boolean notificationsEnabled();

	boolean previewEnabled();

	boolean soundEnabled();

	/**
	 * @return {@code true} when a notification should also be shown while the window is focused.
	 */
	boolean notifyWhenFocused();

	boolean applicationFocused();
}
