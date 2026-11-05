package com.nilhcem.fakesmtp.notification;

/**
 * Shows one notification. Implementations must not throw into the caller.
 *
 * @since 2.1
 */
public interface NotificationBackend {

	/**
	 * @param title the notification title.
	 * @param body the notification body.
	 * @param messageId the inbox id to select when the notification is activated.
	 */
	void show(String title, String body, String messageId);
}
