package com.nilhcem.fakesmtp.mail;

import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.Scrollable;
import javax.swing.SwingConstants;

import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.simple.XHTMLPanel;

/**
 * HTML panel that reflows to the viewport width and scrolls vertically like a mail client.
 *
 * @since 2.1
 */
public final class ScrollableHtmlPanel extends XHTMLPanel implements Scrollable {

	private static final int UNIT_INCREMENT = 16;
	private static final int BLOCK_INCREMENT = 80;

	/**
	 * @param agent the image and resource loader used while painting the message.
	 */
	public ScrollableHtmlPanel(UserAgentCallback agent) {
		super(agent);
		setOpaque(true);
	}

	@Override
	public Dimension getPreferredScrollableViewportSize() {
		return getPreferredSize();
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
		return UNIT_INCREMENT;
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
		if (orientation == SwingConstants.VERTICAL && visible != null && visible.height > 0) {
			return Math.max(BLOCK_INCREMENT, visible.height - BLOCK_INCREMENT);
		}
		return BLOCK_INCREMENT;
	}

	@Override
	public boolean getScrollableTracksViewportWidth() {
		java.awt.Container parent = getParent();
		if (!(parent instanceof javax.swing.JViewport)) {
			return true;
		}
		int viewportWidth = parent.getWidth();
		int preferredWidth = getPreferredSize().width;
		return preferredWidth <= viewportWidth;
	}

	@Override
	public boolean getScrollableTracksViewportHeight() {
		return false;
	}
}
