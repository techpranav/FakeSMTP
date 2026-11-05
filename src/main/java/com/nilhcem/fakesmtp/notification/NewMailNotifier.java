package com.nilhcem.fakesmtp.notification;

import com.nilhcem.fakesmtp.core.Texts;

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
			StringBuilder body = new StringBuilder();
			boolean privacy = preferences.privacyMode();
			if (preferences.showSender()) {
				body.append("From: ").append(blank(message.getParsed().senderLine()));
			}
			if (!privacy && preferences.showSubject()) {
				String subject = message.getParsed().getSubject();
				if (Texts.isBlank(subject)) {
					subject = "(no subject)";
				}
				if (body.length() > 0) {
					body.append('\n');
				}
				body.append("Subject: ").append(subject);
			} else if (privacy) {
				if (body.length() > 0) {
					body.append('\n');
				}
				body.append("New message (preview hidden)");
			}
			if (!privacy && preferences.previewEnabled()) {
				String preview = message.getParsed().getPreview();
				if (!Texts.isBlank(preview)) {
					if (body.length() > 0) {
						body.append('\n');
					}
					body.append(preview);
				}
			}
			if (body.length() == 0) {
				body.append("New email received");
			}
			backend.show("New email received", body.toString(), message.getId());
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
		if (Texts.isBlank(value)) {
			return "(unknown)";
		}
		return value;
	}
}
