package com.nilhcem.fakesmtp.notification;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * A small panel inside the window, used when the desktop cannot show a notification.
 *
 * @since 2.1
 */
public final class InAppToast extends JPanel {

	private static final int HIDE_AFTER_MS = 8000;
	private static final int PADDING = 10;

	private final JLabel titleLabel = new JLabel(" ");
	private final JLabel bodyLabel = new JLabel(" ");
	private final Timer timer;
	private String messageId;
	private ToastHandler handler;

	public InAppToast() {
		super(new BorderLayout(PADDING, 2));
		setVisible(false);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
		add(titleLabel, BorderLayout.NORTH);
		add(bodyLabel, BorderLayout.CENTER);
		timer = new Timer(HIDE_AFTER_MS, event -> setVisible(false));
		timer.setRepeats(false);
		addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent event) {
				if (handler != null && messageId != null) {
					handler.toastActivated(messageId);
				}
				setVisible(false);
			}
		});
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
		setVisible(true);
		timer.restart();
	}

	private static String toHtml(String body) {
		String escaped = body.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
		return "<html>" + escaped.replace("\n", "<br>") + "</html>";
	}

	/**
	 * Receives a click on the in-application notification.
	 */
	public interface ToastHandler {
		void toastActivated(String messageId);
	}
}
