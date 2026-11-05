package com.nilhcem.fakesmtp.notification;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

import com.nilhcem.fakesmtp.core.UISettings;
import com.nilhcem.fakesmtp.gui.inbox.InboxText;
import com.nilhcem.fakesmtp.gui.inbox.UiTokens;

/**
 * Compact in-window toast for new mail. Theme-aware; does not occupy the status bar.
 *
 * @since 2.1
 */
public final class InAppToast extends JPanel {

	private static final int DEFAULT_HIDE_MS = 8000;

	private final JLabel titleLabel = new JLabel(" ");
	private final JLabel bodyLabel = new JLabel(" ");
	private final JButton openButton = new JButton(InboxText.get("toast.open"));
	private final JButton dismissButton = new JButton(InboxText.get("toast.dismiss"));
	private final Timer timer;
	private String messageId;
	private ToastHandler handler;

	public InAppToast() {
		super(new BorderLayout(8, 4));
		setVisible(false);
		setOpaque(true);
		setPreferredSize(new Dimension(UiTokens.TOAST_WIDTH, 92));
		setMaximumSize(new Dimension(UiTokens.TOAST_WIDTH, 120));
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 12.5f));
		bodyLabel.setFont(UiTokens.compactFont(bodyLabel.getFont()));
		openButton.putClientProperty("JButton.buttonType", "toolBarButton");
		dismissButton.putClientProperty("JButton.buttonType", "toolBarButton");
		openButton.addActionListener(event -> activate());
		dismissButton.addActionListener(event -> hideToast());
		JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
		actions.setOpaque(false);
		actions.add(openButton);
		actions.add(dismissButton);
		add(titleLabel, BorderLayout.NORTH);
		add(bodyLabel, BorderLayout.CENTER);
		add(actions, BorderLayout.SOUTH);
		timer = new Timer(hideDelayMs(), event -> hideToast());
		timer.setRepeats(false);
		addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent event) {
				activate();
			}
		});
		applyTheme();
	}

	public void applyTheme() {
		setBackground(UiTokens.TOAST_BG);
		setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(UiTokens.TOAST_BORDER),
				BorderFactory.createEmptyBorder(10, 12, 8, 12)));
		titleLabel.setForeground(UiTokens.TOAST_TITLE);
		bodyLabel.setForeground(UiTokens.TOAST_BODY);
	}

	public void setHandler(ToastHandler handler) {
		this.handler = handler;
	}

	public ToastHandler getHandler() {
		return handler;
	}

	public void showToast(String title, String body, String id) {
		titleLabel.setText(title);
		bodyLabel.setText(toHtml(body));
		messageId = id;
		applyTheme();
		timer.setInitialDelay(hideDelayMs());
		setVisible(true);
		revalidate();
		repaint();
		timer.restart();
	}

	private void activate() {
		if (handler != null && messageId != null) {
			handler.toastActivated(messageId);
		}
		hideToast();
	}

	private void hideToast() {
		setVisible(false);
		timer.stop();
	}

	private static int hideDelayMs() {
		int seconds = UISettings.INSTANCE.getInt(UISettings.NOTIFICATIONS_TOAST_SECONDS, DEFAULT_HIDE_MS / 1000);
		if (seconds < 2) {
			seconds = 2;
		}
		if (seconds > 60) {
			seconds = 60;
		}
		return seconds * 1000;
	}

	private static String toHtml(String body) {
		String escaped = body.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
		return "<html><body style='width:280px'>" + escaped.replace("\n", "<br>") + "</body></html>";
	}

	/**
	 * Receives a click on the in-application notification.
	 */
	public interface ToastHandler {
		void toastActivated(String messageId);
	}
}
