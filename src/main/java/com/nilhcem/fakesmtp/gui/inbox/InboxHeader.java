package com.nilhcem.fakesmtp.gui.inbox;

import com.nilhcem.fakesmtp.core.Texts;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nilhcem.fakesmtp.core.UISettings;
import com.nilhcem.fakesmtp.model.UIModel;

import net.miginfocom.swing.MigLayout;

/**
 * The top bar: application name, server status, port, and window actions.
 *
 * @since 2.1
 */
public final class InboxHeader extends JPanel {

	private static final Logger LOGGER = LoggerFactory.getLogger(InboxHeader.class);
	private static final int STATUS_PAD = 8;

	private final LegacyControls controls;
	private final JLabel status = new JLabel(" ", SwingConstants.CENTER);
	private final JToggleButton notifications = new JToggleButton(InboxText.get("header.notifications"));
	private final JComboBox<String> theme = new JComboBox<String>(new String[] {
		InboxText.get("header.theme.light"), InboxText.get("header.theme.dark")
	});
	private Runnable settingsAction;
	private Runnable themeChanged;
	private Runnable copyConnectionAction;
	private Runnable testEmailAction;

	public InboxHeader(LegacyControls controls) {
		super(new MigLayout("insets 10 14 10 14", "[]12[]12[]8[]8[]8[]8[]push[]8[]8[]8[]", "[]"));
		this.controls = controls;
		setOpaque(true);
		setBackground(new Color(0xFFFFFF));
		setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xE0E3E7)));
		JLabel brand = new JLabel(InboxText.get("header.brand"));
		brand.setFont(brand.getFont().deriveFont(Font.BOLD, brand.getFont().getSize() + 4f));
		brand.setForeground(new Color(0x0F766E));
		JLabel tagline = new JLabel(InboxText.get("header.tagline"));
		tagline.setForeground(new Color(0x5F6368));
		tagline.setFont(tagline.getFont().deriveFont(Font.PLAIN, Math.max(10f, tagline.getFont().getSize() - 1f)));
		JPanel brandBlock = new JPanel(new java.awt.BorderLayout(0, 0));
		brandBlock.setOpaque(false);
		brandBlock.add(brand, java.awt.BorderLayout.NORTH);
		brandBlock.add(tagline, java.awt.BorderLayout.SOUTH);
		status.setOpaque(true);
		status.setBorder(BorderFactory.createEmptyBorder(4, STATUS_PAD, 4, STATUS_PAD));
		status.putClientProperty("JComponent.roundRect", Boolean.TRUE);
		controls.getPort().get().setColumns(5);
		JButton settings = new JButton(InboxText.get("header.settings"));
		settings.addActionListener(event -> {
			if (settingsAction != null) {
				settingsAction.run();
			}
		});
		JButton copyConnection = new JButton(InboxText.get("header.copyConnection"));
		copyConnection.setToolTipText(InboxText.get("header.copyConnection.tip"));
		copyConnection.addActionListener(event -> {
			if (copyConnectionAction != null) {
				copyConnectionAction.run();
			}
		});
		JButton testEmail = new JButton(InboxText.get("header.testEmail"));
		testEmail.setToolTipText(InboxText.get("header.testEmail.tip"));
		testEmail.addActionListener(event -> {
			if (testEmailAction != null) {
				testEmailAction.run();
			}
		});
		notifications.setSelected(UISettings.INSTANCE.getBoolean(UISettings.NOTIFICATIONS_ENABLED));
		notifications.addActionListener(event -> saveNotifications());
		if (UISettings.INSTANCE.isDarkTheme()) {
			theme.setSelectedIndex(1);
		}
		theme.addActionListener(event -> {
			if (themeChanged != null) {
				themeChanged.run();
			}
		});
		add(brandBlock);
		add(status);
		add(new JLabel(InboxText.get("header.port")));
		add(controls.getPort().get(), "w 72!");
		add(controls.getServer().get(), "sg server");
		add(controls.getServer().getStopButton(), "sg server");
		add(testEmail);
		add(copyConnection);
		add(notifications);
		add(theme);
		add(settings);
		syncServerState();
	}

	public void setSettingsAction(Runnable settingsAction) {
		this.settingsAction = settingsAction;
	}

	public void setThemeChanged(Runnable themeChanged) {
		this.themeChanged = themeChanged;
	}

	public void setCopyConnectionAction(Runnable copyConnectionAction) {
		this.copyConnectionAction = copyConnectionAction;
	}

	public void setTestEmailAction(Runnable testEmailAction) {
		this.testEmailAction = testEmailAction;
	}

	public boolean isDarkSelected() {
		return theme.getSelectedIndex() == 1;
	}

	public void syncServerState() {
		boolean running = UIModel.INSTANCE.isStarted();
		String text = InboxText.get("header.status.stopped");
		Color background = new Color(0xF1F3F4);
		Color foreground = new Color(0x5F6368);
		if (controls.getServer().isStartFailed() && !running) {
			text = InboxText.get("header.status.error");
			background = new Color(0xFCE8E6);
			foreground = new Color(0xC5221F);
		} else if (running) {
			text = InboxText.get("header.status.running");
			String port = controls.getPort().get().getText();
			if (!Texts.isBlank(port)) {
				text = text + "  " + port;
			}
			background = new Color(0xE6F4EA);
			foreground = new Color(0x137333);
		}
		status.setText("  " + text + "  ");
		status.setBackground(background);
		status.setForeground(foreground);
	}

	public void showTransientStatus(String key) {
		status.setText("  " + InboxText.get(key) + "  ");
		status.setBackground(new Color(0xFEF7E0));
		status.setForeground(new Color(0xB06000));
		status.paintImmediately(status.getVisibleRect());
	}

	private void saveNotifications() {
		UISettings.INSTANCE.setBoolean(UISettings.NOTIFICATIONS_ENABLED, notifications.isSelected());
		try {
			UISettings.INSTANCE.save();
		} catch (IOException e) {
			LOGGER.warn("Could not save the notification switch", e);
		}
	}

	public void refreshNotificationSwitch() {
		notifications.setSelected(UISettings.INSTANCE.getBoolean(UISettings.NOTIFICATIONS_ENABLED));
	}
}
