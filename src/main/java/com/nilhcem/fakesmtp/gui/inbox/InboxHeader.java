package com.nilhcem.fakesmtp.gui.inbox;

import com.nilhcem.fakesmtp.core.Texts;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
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
 * Compact application chrome: brand, secondary SMTP status, and window actions.
 *
 * @since 2.1
 */
public final class InboxHeader extends JPanel {

	private static final Logger LOGGER = LoggerFactory.getLogger(InboxHeader.class);

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
		super(new MigLayout("insets 8 14 8 14", "[]14[grow][]", "[]"));
		this.controls = controls;
		setOpaque(true);
		setBackground(UiTokens.SURFACE);
		setBorder(UiTokens.bottomHairline());
		setPreferredSize(new Dimension(10, UiTokens.HEADER_HEIGHT));

		JLabel brand = new JLabel(InboxText.get("header.brand"));
		brand.setFont(UiTokens.brandFont(brand.getFont()));
		brand.setForeground(UiTokens.ACCENT);
		JLabel tagline = new JLabel(InboxText.get("header.tagline"));
		tagline.setForeground(UiTokens.TEXT_SECONDARY);
		tagline.setFont(UiTokens.secondaryFont(tagline.getFont()));
		JPanel brandBlock = new JPanel(new BorderLayout(0, 0));
		brandBlock.setOpaque(false);
		brandBlock.add(brand, BorderLayout.NORTH);
		brandBlock.add(tagline, BorderLayout.SOUTH);

		status.setOpaque(true);
		status.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
		status.putClientProperty("JComponent.roundRect", Boolean.TRUE);
		controls.getPort().get().setColumns(4);
		controls.getPort().get().putClientProperty("JComponent.roundRect", Boolean.TRUE);

		JPanel smtp = new JPanel(new MigLayout("insets 0, gap 6", "[][][][][][]", "[]"));
		smtp.setOpaque(false);
		smtp.add(status);
		smtp.add(new JLabel(InboxText.get("header.port")));
		smtp.add(controls.getPort().get(), "w 64!");
		smtp.add(controls.getServer().get());
		smtp.add(controls.getServer().getStopButton());
		JButton testEmail = quietButton(InboxText.get("header.testEmail"));
		testEmail.setToolTipText(InboxText.get("header.testEmail.tip"));
		testEmail.addActionListener(event -> {
			if (testEmailAction != null) {
				testEmailAction.run();
			}
		});
		JButton copyConnection = quietButton(InboxText.get("header.copyConnection"));
		copyConnection.setToolTipText(InboxText.get("header.copyConnection.tip"));
		copyConnection.addActionListener(event -> {
			if (copyConnectionAction != null) {
				copyConnectionAction.run();
			}
		});
		smtp.add(testEmail);
		smtp.add(copyConnection);

		JButton settings = quietButton(InboxText.get("header.settings"));
		settings.addActionListener(event -> {
			if (settingsAction != null) {
				settingsAction.run();
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

		JPanel actions = new JPanel(new MigLayout("insets 0, gap 6", "[][][]", "[]"));
		actions.setOpaque(false);
		actions.add(notifications);
		actions.add(theme);
		actions.add(settings);

		add(brandBlock);
		add(smtp, "grow");
		add(actions);
		syncServerState();
	}

	private static JButton quietButton(String text) {
		JButton button = new JButton(text);
		button.putClientProperty("JButton.buttonType", "toolBarButton");
		return button;
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
		Color background = UiTokens.MUTED_BG;
		Color foreground = UiTokens.TEXT_SECONDARY;
		if (controls.getServer().isStartFailed() && !running) {
			text = "\u25CF  " + InboxText.get("header.status.error");
			background = UiTokens.ERROR_BG;
			foreground = UiTokens.ERROR_FG;
		} else if (running) {
			String port = controls.getPort().get().getText();
			text = "\u25CF  " + InboxText.get("header.status.running");
			if (!Texts.isBlank(port)) {
				text = text + "  :" + port.trim();
			}
			background = UiTokens.SUCCESS_BG;
			foreground = UiTokens.SUCCESS_FG;
		} else {
			text = "\u25CF  " + text;
		}
		status.setText(text);
		status.setBackground(background);
		status.setForeground(foreground);
		status.setFont(status.getFont().deriveFont(Font.BOLD, 12f));
	}

	public void showTransientStatus(String key) {
		status.setText("\u25CF  " + InboxText.get(key));
		status.setBackground(UiTokens.WARNING_BG);
		status.setForeground(UiTokens.WARNING_FG);
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
