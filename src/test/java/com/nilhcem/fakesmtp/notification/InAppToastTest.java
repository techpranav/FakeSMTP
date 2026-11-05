package com.nilhcem.fakesmtp.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.Test;

public class InAppToastTest {

	@Test
	public void dismissDoesNotActivateHandler() throws Exception {
		final InAppToast toast = new InAppToast();
		final AtomicReference<String> opened = new AtomicReference<String>();
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				toast.setHandler(new InAppToast.ToastHandler() {
					@Override
					public void toastActivated(String messageId) {
						opened.set(messageId);
					}
				});
				toast.showToast("New email received", "From: a@b.c\nSubject: Hello", "msg-1");
				assertTrue(toast.isVisible());
				assertTrue(toast.getPreferredSize().width <= 420);
				assertTrue(toast.getPreferredSize().height <= 140);
				clickButton(toast, "Dismiss");
			}
		});
		assertFalse(toast.isVisible());
		assertEquals(null, opened.get());
	}

	private static void clickButton(java.awt.Container root, String text) {
		for (java.awt.Component child : root.getComponents()) {
			if (child instanceof javax.swing.JButton && text.equals(((javax.swing.JButton) child).getText())) {
				((javax.swing.JButton) child).doClick();
				return;
			}
			if (child instanceof java.awt.Container) {
				clickButton((java.awt.Container) child, text);
			}
		}
	}

	@Test
	public void queuesAdditionalToastsWhileVisible() throws Exception {
		final InAppToast toast = new InAppToast();
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				toast.showToast("New email received", "From: one@example.com\nSubject: One", "1");
				toast.showToast("New email received", "From: two@example.com\nSubject: Two", "2");
				assertTrue(toast.isVisible());
			}
		});
	}

	@Test
	public void openActivatesQueuedHandler() throws Exception {
		final InAppToast toast = new InAppToast();
		final AtomicReference<String> opened = new AtomicReference<String>();
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				toast.setHandler(new InAppToast.ToastHandler() {
					@Override
					public void toastActivated(String messageId) {
						opened.set(messageId);
					}
				});
				toast.showToast("New email received", "From: a@b.c\nSubject: Hello", "msg-open");
				clickButton(toast, "Open");
			}
		});
		assertEquals("msg-open", opened.get());
		assertFalse(toast.isVisible());
	}
}
