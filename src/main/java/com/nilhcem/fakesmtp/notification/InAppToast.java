package com.nilhcem.fakesmtp.notification;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayDeque;
import java.util.Deque;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.nilhcem.fakesmtp.core.UISettings;
import com.nilhcem.fakesmtp.gui.inbox.InboxText;
import com.nilhcem.fakesmtp.gui.inbox.UiTokens;

/**
 * Compact floating in-window toast. Does not participate in the main BorderLayout;
 * host it on a layered/overlay pane so it never reserves workspace height.
 *
 * @since 2.1
 */
public final class InAppToast extends JPanel {

	private static final int DEFAULT_HIDE_MS = 8000;
	private static final int MAX_QUEUE = 8;

	private final JLabel brand = new JLabel(InboxText.get("header.brand"));
	private final JLabel titleLabel = new JLabel(" ");
	private final JLabel bodyLabel = new JLabel(" ");
	private final JButton openButton = new JButton(InboxText.get("toast.open"));
	private final JButton dismissButton = new JButton(InboxText.get("toast.dismiss"));
	private final Timer timer;
	private final Deque<Pending> queue = new ArrayDeque<Pending>();
	private String messageId;
	private ToastHandler handler;
	private boolean showing;

	public InAppToast() {
		super(new BorderLayout(8, 4));
		setVisible(false);
		setOpaque(true);
		setFocusable(false);
		Dimension size = new Dimension(UiTokens.TOAST_WIDTH, 108);
		setPreferredSize(size);
		setMinimumSize(size);
		setMaximumSize(size);
		brand.setFont(UiTokens.compactFont(brand.getFont()).deriveFont(Font.BOLD));
		java.net.URL iconUrl = InAppToast.class.getResource("/icon-16.png");
		if (iconUrl != null) {
			brand.setIcon(new ImageIcon(iconUrl));
			brand.setIconTextGap(6);
		}
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 12.5f));
		bodyLabel.setFont(UiTokens.compactFont(bodyLabel.getFont()));
		openButton.putClientProperty("JButton.buttonType", "toolBarButton");
		dismissButton.putClientProperty("JButton.buttonType", "toolBarButton");
		openButton.addActionListener(event -> activate());
		dismissButton.addActionListener(event -> dismissOnly());
		JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
		actions.setOpaque(false);
		actions.add(openButton);
		actions.add(dismissButton);
		JPanel north = new JPanel(new BorderLayout());
		north.setOpaque(false);
		north.add(brand, BorderLayout.WEST);
		north.add(titleLabel, BorderLayout.SOUTH);
		add(north, BorderLayout.NORTH);
		add(bodyLabel, BorderLayout.CENTER);
		add(actions, BorderLayout.SOUTH);
		timer = new Timer(hideDelayMs(), event -> hideAndShowNext());
		timer.setRepeats(false);
		addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent event) {
				if (!(event.getSource() instanceof JButton)) {
					activate();
				}
			}
		});
		applyTheme();
	}

	public void applyTheme() {
		setBackground(UiTokens.TOAST_BG);
		setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(UiTokens.TOAST_BORDER),
				BorderFactory.createEmptyBorder(10, 12, 8, 12)));
		brand.setForeground(UiTokens.ACCENT);
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
		if (!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(() -> showToast(title, body, id));
			return;
		}
		Pending next = new Pending(title, body, id);
		if (showing) {
			while (queue.size() >= MAX_QUEUE) {
				queue.pollFirst();
			}
			queue.addLast(next);
			if (queue.size() > 1) {
				titleLabel.setText(InboxText.format("toast.queued", Integer.valueOf(queue.size() + 1)));
			}
			return;
		}
		display(next);
	}

	private void display(Pending pending) {
		titleLabel.setText(pending.title);
		bodyLabel.setText(toHtml(pending.body));
		messageId = pending.id;
		showing = true;
		applyTheme();
		timer.setInitialDelay(hideDelayMs());
		setVisible(true);
		revalidate();
		repaint();
		java.awt.Container parent = getParent();
		if (parent != null) {
			parent.revalidate();
			parent.repaint();
		}
		timer.restart();
	}

	private void activate() {
		String id = messageId;
		hideCurrent();
		if (handler != null && id != null) {
			handler.toastActivated(id);
		}
		showNextIfAny();
	}

	/** Dismiss without opening or marking the email read. */
	private void dismissOnly() {
		hideAndShowNext();
	}

	private void hideAndShowNext() {
		hideCurrent();
		showNextIfAny();
	}

	private void hideCurrent() {
		timer.stop();
		showing = false;
		messageId = null;
		setVisible(false);
		java.awt.Container parent = getParent();
		if (parent != null) {
			parent.revalidate();
			parent.repaint();
		}
	}

	private void showNextIfAny() {
		Pending next = queue.pollFirst();
		if (next != null) {
			display(next);
		}
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

	private static final class Pending {
		private final String title;
		private final String body;
		private final String id;

		private Pending(String title, String body, String id) {
			this.title = title;
			this.body = body;
			this.id = id;
		}
	}

	/**
	 * Receives Open / click-to-open on the in-application notification.
	 */
	public interface ToastHandler {
		void toastActivated(String messageId);
	}
}
