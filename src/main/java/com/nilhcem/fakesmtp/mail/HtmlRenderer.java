package com.nilhcem.fakesmtp.mail;

import com.nilhcem.fakesmtp.core.Texts;

import java.awt.Color;
import java.awt.Dimension;
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

	public static ScrollableHtmlPanel createPanel(MailUserAgent agent) {
		ScrollableHtmlPanel panel = new ScrollableHtmlPanel(agent);
		panel.setInteractive(true);
		panel.setBackground(Color.WHITE);
		removeDefaultLinks(panel);
		return panel;
	}

	public static void show(XHTMLPanel panel, MailUserAgent agent, ParsedEmail email) {
		if (email == null || Texts.isBlank(email.getPreparedHtml())) {
			throw new IllegalArgumentException("This message has no HTML body");
		}
		agent.setInlineImages(email.getInlineImages());
		agent.clearImageCache();
		// Flying Saucer's ImageResourceLoader opens img src as URLs and ignores the user agent
		// for non-data URIs, so cid: references must be rewritten to data URLs before paint.
		String html = CidEmbedder.embed(email.getPreparedHtml(), email.getInlineImages());
		panel.setDocumentFromString(html, null, new XhtmlNamespaceHandler());
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
		ScrollableHtmlPanel panel = createPanel(agent);
		show(panel, agent, email);
		panel.setSize(new Dimension(width, height));
		panel.doLayout();
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

	/**
	 * Renders the full HTML body at the given width, growing the image height to fit long messages.
	 *
	 * @param email prepared message.
	 * @param width image width in pixels.
	 * @param maxHeight maximum image height (taller content is clipped at this limit).
	 * @return the rendered image.
	 */
	public static BufferedImage renderFull(ParsedEmail email, int width, int maxHeight) {
		MailUserAgent agent = new MailUserAgent();
		ScrollableHtmlPanel panel = createPanel(agent);
		show(panel, agent, email);
		int preferred = measureContentHeight(panel, width, maxHeight);
		panel.setSize(new Dimension(width, preferred));
		panel.doLayout();
		BufferedImage image = new BufferedImage(width, preferred, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		try {
			graphics.setColor(Color.WHITE);
			graphics.fillRect(0, 0, width, preferred);
			panel.paint(graphics);
		} finally {
			graphics.dispose();
		}
		return image;
	}

	/**
	 * Flying Saucer's {@code getPreferredSize()} often under-reports tall HTML (tables/SVG).
	 * Force a document layout pass, then take the root box height.
	 */
	private static int measureContentHeight(ScrollableHtmlPanel panel, int width, int maxHeight) {
		int layoutHeight = Math.min(Math.max(2400, maxHeight / 2), maxHeight);
		panel.setSize(new Dimension(width, layoutHeight));
		BufferedImage probe = new BufferedImage(width, 64, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = probe.createGraphics();
		try {
			panel.doDocumentLayout(graphics);
			panel.doLayout();
		} finally {
			graphics.dispose();
		}
		int height = 200;
		org.xhtmlrenderer.render.Box root = panel.getRootBox();
		if (root != null) {
			height = Math.max(height, root.getHeight() + Math.max(0, root.getY()) + 32);
		}
		Dimension preferred = panel.getPreferredSize();
		if (preferred != null) {
			height = Math.max(height, preferred.height + 16);
		}
		if (height > maxHeight) {
			throw new IllegalArgumentException("Email content height " + height
					+ "px exceeds the capture limit of " + maxHeight + "px");
		}
		return Math.max(height, 200);
	}

	private static void removeDefaultLinks(XHTMLPanel panel) {
		for (Object listener : panel.getMouseTrackingListeners().toArray()) {
			if (listener instanceof LinkListener) {
				panel.removeMouseTrackingListener((FSMouseListener) listener);
			}
		}
	}
}
