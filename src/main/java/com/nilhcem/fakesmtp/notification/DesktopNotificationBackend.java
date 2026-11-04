package com.nilhcem.fakesmtp.notification;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shows a native notification when the desktop supports one, and an in-window toast otherwise.
 * <p>
 * On Windows and macOS the system tray balloon is used. On Linux, {@code notify-send} is tried
 * when there is no tray. Activating a tray balloon or the in-window toast selects the message.
 * {@code notify-send} cannot report which notification was clicked.
 * </p>
 *
 * @since 2.1
 */
public final class DesktopNotificationBackend implements NotificationBackend {

	private static final Logger LOGGER = LoggerFactory.getLogger(DesktopNotificationBackend.class);
	private static final int NOTIFY_SEND_WAIT_SECONDS = 2;

	private final InAppToast toast;
	private final Image icon;
	private TrayIcon trayIcon;
	private String latestId;
	private Boolean notifySendAvailable;

	public DesktopNotificationBackend(InAppToast toast, Image icon) {
		this.toast = toast;
		this.icon = icon;
	}

	@Override
	public void show(String title, String body, String messageId) {
		latestId = messageId;
		try {
			if (showTray(title, body)) {
				return;
			}
			if (showNotifySend(title, body)) {
				return;
			}
		} catch (RuntimeException e) {
			LOGGER.warn("Desktop notification failed; using the in-application notice", e);
		}
		SwingUtilities.invokeLater(() -> toast.showToast(title, body, messageId));
	}

	private boolean showTray(String title, String body) {
		if (!SystemTray.isSupported() || icon == null) {
			return false;
		}
		try {
			ensureTrayIcon();
			trayIcon.displayMessage(title, body, TrayIcon.MessageType.INFO);
			return true;
		} catch (AWTException e) {
			LOGGER.warn("Could not add a system tray icon", e);
			return false;
		}
	}

	private void ensureTrayIcon() throws AWTException {
		if (trayIcon != null) {
			return;
		}
		trayIcon = new TrayIcon(icon, "FakeSMTP");
		trayIcon.setImageAutoSize(true);
		trayIcon.addActionListener(event -> {
			if (toast.getHandler() != null && latestId != null) {
				toast.getHandler().toastActivated(latestId);
			}
		});
		SystemTray.getSystemTray().add(trayIcon);
	}

	private boolean showNotifySend(String title, String body) {
		if (!isNotifySendAvailable()) {
			return false;
		}
		try {
			new ProcessBuilder("notify-send", "-a", "FakeSMTP", title, body).start();
			return true;
		} catch (IOException e) {
			LOGGER.warn("notify-send failed", e);
			notifySendAvailable = Boolean.FALSE;
			return false;
		}
	}

	private boolean isNotifySendAvailable() {
		if (notifySendAvailable != null) {
			return notifySendAvailable.booleanValue();
		}
		notifySendAvailable = Boolean.FALSE;
		String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		if (os.contains("win") || os.contains("mac")) {
			return false;
		}
		try {
			Process process = new ProcessBuilder("notify-send", "--version").start();
			if (process.waitFor(NOTIFY_SEND_WAIT_SECONDS, TimeUnit.SECONDS) && process.exitValue() == 0) {
				notifySendAvailable = Boolean.TRUE;
			}
		} catch (Exception e) {
			LOGGER.debug("notify-send is not available", e);
		}
		return notifySendAvailable.booleanValue();
	}
}
