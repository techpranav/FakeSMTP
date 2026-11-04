package com.nilhcem.fakesmtp.notification;

import java.awt.Toolkit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nilhcem.fakesmtp.model.InboxMessage;

/**
 * Decides whether a newly received message should raise a notification.
 * <p>
 * Failures are logged and never thrown, so a desktop notification problem cannot affect SMTP.
 * </p>
 *
 * @since 2.1
 */
public final class NewMailNotifier {

	private static final Logger LOGGER = LoggerFactory.getLogger(NewMailNotifier.class);

	private final NotificationPreferences preferences;
	private final NotificationBackend backend;

	public NewMailNotifier(NotificationPreferences preferences, NotificationBackend backend) {
		this.preferences = preferences;
		this.backend = backend;
	}

	public void announce(InboxMessage message) {
		try {
			if (message == null || message.getParsed() == null || !preferences.notificationsEnabled()) {
				return;
			}
			if (preferences.applicationFocused() && !preferences.notifyWhenFocused()) {
				return;
			}
			String subject = message.getParsed().getSubject();
			if (subject == null || subject.isBlank()) {
				subject = "(no subject)";
			}
			String body = "From: " + blank(message.getParsed().senderLine()) + "\nSubject: " + subject;
			if (preferences.previewEnabled()) {
				String preview = message.getParsed().getPreview();
				if (preview != null && !preview.isBlank()) {
					body = body + "\n" + preview;
				}
			}
			backend.show("New email received", body, message.getId());
			if (preferences.soundEnabled()) {
				beep();
			}
		} catch (RuntimeException e) {
			LOGGER.warn("Could not show a new-email notification", e);
		}
	}

	private static void beep() {
		try {
			Toolkit.getDefaultToolkit().beep();
		} catch (RuntimeException e) {
			LOGGER.debug("Notification sound is not available", e);
		}
	}

	private static String blank(String value) {
		if (value == null || value.isBlank()) {
			return "(unknown)";
		}
		return value;
	}
}
