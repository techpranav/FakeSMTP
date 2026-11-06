package com.nilhcem.fakesmtp.gui.inbox;

import java.awt.Component;
import java.awt.Font;
import java.io.IOException;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nilhcem.fakesmtp.core.UISettings;

import net.miginfocom.swing.MigLayout;

/**
 * Sectioned preferences dialog. Preserves existing setting keys and files.
 *
 * @since 2.1
 */
public final class SettingsDialog {

	private static final Logger LOGGER = LoggerFactory.getLogger(SettingsDialog.class);

	private SettingsDialog() {
		throw new UnsupportedOperationException();
	}

	/**
	 * @param parent the window that owns the dialog.
	 * @param testNotification optional action that fires a sample desktop notification.
	 * @return {@code true} when the user saved the settings.
	 */
	public static boolean show(Component parent, Runnable testNotification) {
		UISettings settings = UISettings.INSTANCE;
		JCheckBox notifications = box("settings.notifications", settings.getBoolean(UISettings.NOTIFICATIONS_ENABLED));
		JCheckBox desktop = box("settings.desktop", settings.getBoolean(UISettings.NOTIFICATIONS_DESKTOP));
		JCheckBox inApp = box("settings.inapp", settings.getBoolean(UISettings.NOTIFICATIONS_INAPP));
		JCheckBox preview = box("settings.preview", settings.getBoolean(UISettings.NOTIFICATIONS_PREVIEW));
		JCheckBox sound = box("settings.sound", settings.getBoolean(UISettings.NOTIFICATIONS_SOUND));
		JCheckBox every = box("settings.every", settings.getBoolean(UISettings.NOTIFICATIONS_EVERY_EMAIL));
		JCheckBox showSender = box("settings.showSender", settings.getBoolean(UISettings.NOTIFICATIONS_SHOW_SENDER));
		JCheckBox showSubject = box("settings.showSubject", settings.getBoolean(UISettings.NOTIFICATIONS_SHOW_SUBJECT));
		JCheckBox privacy = box("settings.privacy", settings.getBoolean(UISettings.NOTIFICATIONS_PRIVACY));
		JCheckBox images = box("settings.images", settings.getBoolean(UISettings.EXTERNAL_IMAGES));
		JCheckBox newest = box("settings.sort", settings.getBoolean(UISettings.SORT_NEWEST_FIRST));
		JCheckBox load = box("settings.load", settings.getBoolean(UISettings.LOAD_SAVED_EMAILS));
		javax.swing.JSpinner toastSeconds = new javax.swing.JSpinner(new javax.swing.SpinnerNumberModel(
				Math.max(2, settings.getInt(UISettings.NOTIFICATIONS_TOAST_SECONDS, 8)), 2, 60, 1));
		JComboBox<String> view = new JComboBox<String>(new String[] {
			InboxText.get("settings.view.html"), InboxText.get("settings.view.text")
		});
		if (UISettings.VIEW_TEXT.equals(settings.get(UISettings.DEFAULT_VIEW))) {
			view.setSelectedIndex(1);
		}
		JPanel form = new JPanel(new MigLayout("wrap 1, insets 8", "[grow]"));
		form.add(section(InboxText.get("settings.section.notifications")));
		form.add(notifications);
		form.add(desktop);
		form.add(inApp);
		form.add(showSender);
		form.add(showSubject);
		form.add(preview);
		form.add(privacy);
		form.add(sound);
		form.add(every);
		form.add(new JLabel(InboxText.get("settings.toast.seconds")));
		form.add(toastSeconds, "w 80!");
		if (testNotification != null) {
			javax.swing.JButton test = new javax.swing.JButton(InboxText.get("settings.testNotification"));
			test.addActionListener(event -> testNotification.run());
			form.add(test);
		}
		form.add(section(InboxText.get("settings.section.display")), "gaptop 10");
		form.add(images);
		form.add(new JLabel(InboxText.get("settings.view")));
		form.add(view, "grow");
		form.add(newest);
		form.add(section(InboxText.get("settings.section.storage")), "gaptop 10");
		form.add(load);
		form.add(new JLabel("<html><body style='width:300px;color:" + UiTokens.toHex(UiTokens.TEXT_SECONDARY)
				+ "'>" + InboxText.get("settings.note") + "</body></html>"));
		JScrollPane scroll = new JScrollPane(form);
		scroll.setBorder(null);
		scroll.setPreferredSize(new java.awt.Dimension(400, 480));
		int choice = JOptionPane.showConfirmDialog(parent, scroll, InboxText.get("settings.title"),
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (choice != JOptionPane.OK_OPTION) {
			return false;
		}
		settings.setBoolean(UISettings.NOTIFICATIONS_ENABLED, notifications.isSelected());
		settings.setBoolean(UISettings.NOTIFICATIONS_DESKTOP, desktop.isSelected());
		settings.setBoolean(UISettings.NOTIFICATIONS_INAPP, inApp.isSelected());
		settings.setBoolean(UISettings.NOTIFICATIONS_SHOW_SENDER, showSender.isSelected());
		settings.setBoolean(UISettings.NOTIFICATIONS_SHOW_SUBJECT, showSubject.isSelected());
		settings.setBoolean(UISettings.NOTIFICATIONS_PREVIEW, preview.isSelected());
		settings.setBoolean(UISettings.NOTIFICATIONS_PRIVACY, privacy.isSelected());
		settings.setBoolean(UISettings.NOTIFICATIONS_SOUND, sound.isSelected());
		settings.setBoolean(UISettings.NOTIFICATIONS_EVERY_EMAIL, every.isSelected());
		settings.setInt(UISettings.NOTIFICATIONS_TOAST_SECONDS, ((Integer) toastSeconds.getValue()).intValue());
		settings.setBoolean(UISettings.EXTERNAL_IMAGES, images.isSelected());
		settings.setBoolean(UISettings.SORT_NEWEST_FIRST, newest.isSelected());
		settings.setBoolean(UISettings.LOAD_SAVED_EMAILS, load.isSelected());
		if (view.getSelectedIndex() == 1) {
			settings.set(UISettings.DEFAULT_VIEW, UISettings.VIEW_TEXT);
		} else {
			settings.set(UISettings.DEFAULT_VIEW, UISettings.VIEW_HTML);
		}
		try {
			settings.save();
		} catch (IOException e) {
			LOGGER.warn("Could not save UI settings", e);
			JOptionPane.showMessageDialog(parent, InboxText.get("settings.save.failed"));
		}
		return true;
	}

	private static JLabel section(String text) {
		JLabel label = new JLabel(text);
		label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
		label.setForeground(UiTokens.TEXT);
		return label;
	}

	private static JCheckBox box(String key, boolean selected) {
		return new JCheckBox(InboxText.get(key), selected);
	}
}
