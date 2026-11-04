package com.nilhcem.fakesmtp.mail;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.xhtmlrenderer.simple.XHTMLPanel;
import org.xhtmlrenderer.simple.extend.XhtmlNamespaceHandler;
import org.xhtmlrenderer.swing.FSMouseListener;
import org.xhtmlrenderer.swing.LinkListener;

/**
 * Draws prepared HTML into the Swing panel used by the reading view.
 *
 * @since 2.1
 */
public final class HtmlRenderer {

	private HtmlRenderer() {
		throw new UnsupportedOperationException();
	}

	public static XHTMLPanel createPanel(MailUserAgent agent) {
		XHTMLPanel panel = new XHTMLPanel(agent);
		panel.setInteractive(true);
		panel.setBackground(Color.WHITE);
		removeDefaultLinks(panel);
		return panel;
	}

	public static void show(XHTMLPanel panel, MailUserAgent agent, ParsedEmail email) {
		if (email == null || email.getPreparedHtml() == null || email.getPreparedHtml().isBlank()) {
			throw new IllegalArgumentException("This message has no HTML body");
		}
		agent.setInlineImages(email.getInlineImages());
		agent.clearImageCache();
		panel.setDocumentFromString(email.getPreparedHtml(), null, new XhtmlNamespaceHandler());
	}

	/**
	 * Paints a message into an image. Used to check that a real email produces visible output.
	 *
	 * @param email the prepared message.
	 * @param width image width in pixels.
	 * @param height image height in pixels.
	 * @return the rendered image.
	 */
	public static BufferedImage render(ParsedEmail email, int width, int height) {
		MailUserAgent agent = new MailUserAgent();
		XHTMLPanel panel = createPanel(agent);
		show(panel, agent, email);
		panel.setSize(width, height);
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		try {
			graphics.setColor(Color.WHITE);
			graphics.fillRect(0, 0, width, height);
			panel.paint(graphics);
		} finally {
			graphics.dispose();
		}
		return image;
	}

	private static void removeDefaultLinks(XHTMLPanel panel) {
		for (Object listener : panel.getMouseTrackingListeners().toArray()) {
			if (listener instanceof LinkListener) {
				panel.removeMouseTrackingListener((FSMouseListener) listener);
			}
		}
	}
}
