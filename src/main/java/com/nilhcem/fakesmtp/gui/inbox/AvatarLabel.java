package com.nilhcem.fakesmtp.gui.inbox;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JComponent;

import com.nilhcem.fakesmtp.core.Texts;

/**
 * Circular initials avatar for list and header chrome (not email HTML).
 *
 * @since 2.1
 */
public final class AvatarLabel extends JComponent {

	private String initials = "?";
	private Color fill = UiTokens.AVATAR_BG;
	private Color ink = UiTokens.AVATAR_FG;

	public AvatarLabel() {
		setOpaque(false);
		int size = UiTokens.AVATAR_SIZE;
		setPreferredSize(new Dimension(size, size));
		setMinimumSize(new Dimension(size, size));
		setMaximumSize(new Dimension(size, size));
	}

	public void setIdentity(String nameOrEmail) {
		initials = initialsOf(nameOrEmail);
		int hash = Math.abs(initials.hashCode());
		fill = new Color(180 + hash % 50, 200 + (hash / 7) % 40, 230 + (hash / 13) % 20);
		ink = UiTokens.AVATAR_FG;
		repaint();
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D g = (Graphics2D) graphics.create();
		try {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			int size = Math.min(getWidth(), getHeight());
			g.setColor(fill);
			g.fillOval(0, 0, size - 1, size - 1);
			g.setColor(ink);
			g.setFont(getFont().deriveFont(Font.BOLD, 12f));
			FontMetrics metrics = g.getFontMetrics();
			int x = (size - metrics.stringWidth(initials)) / 2;
			int y = (size - metrics.getHeight()) / 2 + metrics.getAscent();
			g.drawString(initials, x, y);
		} finally {
			g.dispose();
		}
	}

	static String initialsOf(String value) {
		if (Texts.isBlank(value)) {
			return "?";
		}
		String cleaned = value.trim();
		int at = cleaned.indexOf('<');
		if (at > 0) {
			cleaned = cleaned.substring(0, at).trim();
		}
		if (cleaned.contains("@")) {
			cleaned = cleaned.substring(0, cleaned.indexOf('@'));
		}
		String[] parts = cleaned.split("[\\s._\\-]+");
		StringBuilder out = new StringBuilder();
		for (String part : parts) {
			if (part.isEmpty()) {
				continue;
			}
			out.append(Character.toUpperCase(part.charAt(0)));
			if (out.length() >= 2) {
				break;
			}
		}
		if (out.length() == 0) {
			return "?";
		}
		if (out.length() == 1 && cleaned.length() > 1) {
			out.append(Character.toUpperCase(cleaned.charAt(1)));
		}
		return out.toString();
	}
}
