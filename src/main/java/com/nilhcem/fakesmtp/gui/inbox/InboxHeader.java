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
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

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
	private final JLabel brand = new JLabel(InboxText.get("header.brand"));
	private final JLabel tagline = new JLabel(InboxText.get("header.tagline"));
	private final JLabel status = new JLabel(" ", SwingConstants.CENTER);
	private final JLabel portLabel = new JLabel(InboxText.get("header.port"));
	private final JToggleButton notifications = new JToggleButton(InboxText.get("header.notifications"));
	private final JComboBox<String> theme = new JComboBox<String>(new String[] {
		InboxText.get("header.theme.light"), InboxText.get("header.theme.dark")
	});
	private Runnable settingsAction;
	private Runnable themeChanged;
	private Runnable copyConnectionAction;
	private Runnable testEmailAction;

	public InboxHeader(LegacyControls controls) {
		super(new MigLayout("insets 6 14 6 14, gapx 10", "[]10[grow]10[]10[]", "[]"));
		this.controls = controls;
		setOpaque(true);
		setPreferredSize(new Dimension(10, UiTokens.HEADER_HEIGHT));

		brand.setFont(UiTokens.brandFont(brand.getFont()));
		tagline.setFont(UiTokens.compactFont(tagline.getFont()));
		JPanel brandBlock = new JPanel(new BorderLayout(0, 0));
		brandBlock.setOpaque(false);
		brandBlock.add(brand, BorderLayout.NORTH);
		brandBlock.add(tagline, BorderLayout.SOUTH);

		status.setOpaque(true);
		status.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
		status.putClientProperty("JComponent.roundRect", Boolean.TRUE);
		controls.getPort().get().setColumns(4);
		controls.getPort().get().putClientProperty("JComponent.roundRect", Boolean.TRUE);
		styleControl(controls.getPort().get());
		styleControl(controls.getServer().get());
		styleControl(controls.getServer().getStopButton());

		JPanel smtp = new JPanel(new MigLayout("insets 0, gap 6", "[][][][][][][]", "[]"));
		smtp.setOpaque(false);
		smtp.add(status);
		portLabel.setFont(UiTokens.secondaryFont(portLabel.getFont()));
		smtp.add(portLabel);
		smtp.add(controls.getPort().get(), "w 56!, h " + UiTokens.CONTROL_HEIGHT + "!");
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
		styleControl(notifications);
		styleControl(theme);
		if (UISettings.INSTANCE.isDarkTheme()) {
			theme.setSelectedIndex(1);
		}
		theme.addActionListener(event -> {
			if (themeChanged != null) {
				// Apply after the combo popup has closed so FlatLaf does not
				// repaint a disposed popup ("this.comboBox is null").
				SwingUtilities.invokeLater(themeChanged);
			}
		});

		JSeparator divider = new JSeparator(SwingConstants.VERTICAL);
		divider.setPreferredSize(new Dimension(1, 22));
		divider.setForeground(UiTokens.BORDER);

		JPanel actions = new JPanel(new MigLayout("insets 0, gap 6", "[][][]", "[]"));
		actions.setOpaque(false);
		actions.add(notifications);
		actions.add(theme, "w 84!");
		actions.add(settings);

		add(brandBlock);
		add(smtp, "grow");
		add(divider, "growy, hmax 24");
		add(actions);
		applyTheme();
		syncServerState();
	}

	public void applyTheme() {
		setBackground(UiTokens.SURFACE);
		setBorder(UiTokens.bottomHairline());
		brand.setForeground(UiTokens.ACCENT);
		tagline.setForeground(UiTokens.TEXT_SECONDARY);
		portLabel.setForeground(UiTokens.TEXT_SECONDARY);
		syncServerState();
	}

	private static JButton quietButton(String text) {
		JButton button = new JButton(text);
		styleButton(button);
		return button;
	}

	private static void styleButton(JButton button) {
		button.putClientProperty("JButton.buttonType", "toolBarButton");
		button.setFont(UiTokens.secondaryFont(button.getFont()));
		Dimension preferred = button.getPreferredSize();
		preferred.height = UiTokens.CONTROL_HEIGHT;
		button.setPreferredSize(preferred);
	}

	private static void styleControl(JComponent component) {
		component.setFont(UiTokens.secondaryFont(component.getFont()));
		Dimension preferred = component.getPreferredSize();
		preferred.height = UiTokens.CONTROL_HEIGHT;
		component.setPreferredSize(preferred);
		if (component instanceof javax.swing.AbstractButton) {
			component.putClientProperty("JButton.buttonType", "toolBarButton");
		}
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
