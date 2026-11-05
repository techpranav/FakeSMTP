package com.nilhcem.fakesmtp.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.nilhcem.fakesmtp.mail.ParsedEmail;
import com.nilhcem.fakesmtp.model.EmailModel;
import com.nilhcem.fakesmtp.model.InboxMessage;

public class NewMailNotifierTest {

	@Test
	public void skipsNotificationWhenDisabled() {
		RecordingBackend backend = new RecordingBackend();
		NewMailNotifier notifier = new NewMailNotifier(preferences(false, true, false, true), backend);
		notifier.announce(message());
		assertTrue(backend.titles.isEmpty());
	}

	@Test
	public void skipsNotificationWhenTheWindowIsFocused() {
		RecordingBackend backend = new RecordingBackend();
		NewMailNotifier notifier = new NewMailNotifier(preferences(true, true, true, false), backend);
		notifier.announce(message());
		assertTrue(backend.titles.isEmpty());
	}

	@Test
	public void notifiesAndKeepsGoingWhenTheBackendFails() {
		NewMailNotifier notifier = new NewMailNotifier(preferences(true, true, false, true), new NotificationBackend() {
			@Override
			public void show(String title, String body, String messageId) {
				throw new IllegalStateException("desktop unavailable");
			}
		});
		notifier.announce(message());
		notifier.announce(message());
	}

	@Test
	public void includesThePreviewWhenRequested() {
		RecordingBackend backend = new RecordingBackend();
		NewMailNotifier notifier = new NewMailNotifier(preferences(true, true, false, true), backend);
		notifier.announce(message());
		assertEquals(1, backend.bodies.size());
		assertTrue(backend.bodies.get(0).contains("From:"));
		assertTrue(backend.bodies.get(0).contains("Password reset"));
		assertTrue(backend.bodies.get(0).contains("please reset"));
	}

	private static InboxMessage message() {
		EmailModel email = new EmailModel();
		email.setEmailStr("raw");
		ParsedEmail parsed = ParsedEmail.builder()
				.subject("Password reset")
				.preview("please reset")
				.envelopeFrom("test@example.com")
				.readable(true)
				.build();
		return new InboxMessage(email, parsed, false);
	}

	private static NotificationPreferences preferences(boolean enabled, boolean preview, boolean focused, boolean notifyWhenFocused) {
		return new NotificationPreferences() {
			@Override
			public boolean notificationsEnabled() {
				return enabled;
			}

			@Override
			public boolean previewEnabled() {
				return preview;
			}

			@Override
			public boolean soundEnabled() {
				return false;
			}

			@Override
			public boolean notifyWhenFocused() {
				return notifyWhenFocused;
			}

			@Override
			public boolean applicationFocused() {
				return focused;
			}
		};
	}

	private static final class RecordingBackend implements NotificationBackend {
		private final List<String> titles = new ArrayList<String>();
		private final List<String> bodies = new ArrayList<String>();

		@Override
		public void show(String title, String body, String messageId) {
			titles.add(title);
			bodies.add(body);
		}
	}
}
