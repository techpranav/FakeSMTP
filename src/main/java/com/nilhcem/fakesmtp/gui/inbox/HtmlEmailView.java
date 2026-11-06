package com.nilhcem.fakesmtp.gui.inbox;

import com.nilhcem.fakesmtp.core.Texts;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhtmlrenderer.swing.BasicPanel;
import org.xhtmlrenderer.swing.LinkListener;

import com.nilhcem.fakesmtp.mail.HtmlRenderer;
import com.nilhcem.fakesmtp.mail.MailUserAgent;
import com.nilhcem.fakesmtp.mail.ParsedEmail;
import com.nilhcem.fakesmtp.mail.ScrollableHtmlPanel;

/**
 * Shows the HTML body of one message, or the plain text when rendering fails.
 *
 * @since 2.1
 */
public final class HtmlEmailView extends JPanel {

	private static final Logger LOGGER = LoggerFactory.getLogger(HtmlEmailView.class);

	private final MailUserAgent agent = new MailUserAgent();
	private final ScrollableHtmlPanel htmlPanel;
	private final JScrollPane htmlScroll;
	private final JTextArea fallback = new JTextArea();
	private final JPanel cards = new JPanel(new java.awt.CardLayout());
	private final JPanel banner = new JPanel(new BorderLayout(8, 0));
	private Runnable loadImages;

	public HtmlEmailView() {
		super(new BorderLayout());
		htmlPanel = HtmlRenderer.createPanel(agent);
		htmlPanel.addMouseTrackingListener(new LinkListener() {
			@Override
			public void linkClicked(BasicPanel panel, String uri) {
				SafeLinks.open(HtmlEmailView.this, uri);
			}
		});
		htmlScroll = new JScrollPane(htmlPanel);
		htmlScroll.setBorder(null);
		htmlScroll.getViewport().setBackground(Color.WHITE);
		htmlScroll.getVerticalScrollBar().setUnitIncrement(16);
		htmlScroll.getHorizontalScrollBar().setUnitIncrement(16);
		htmlScroll.getViewport().addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent event) {
				reflowHtml();
			}
		});
		fallback.setEditable(false);
		fallback.setLineWrap(true);
		fallback.setWrapStyleWord(true);
		JButton load = new JButton(InboxText.get("reader.images.load"));
		load.addActionListener(event -> {
			if (loadImages != null) {
				loadImages.run();
			}
		});
		banner.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 12, 6, 12));
		banner.add(new JLabel(InboxText.get("reader.images.blocked")), BorderLayout.CENTER);
		banner.add(load, BorderLayout.EAST);
		banner.setVisible(false);
		cards.add(htmlScroll, "html");
		cards.add(new JScrollPane(fallback), "text");
		add(banner, BorderLayout.NORTH);
		add(cards, BorderLayout.CENTER);
	}

	public void setLoadImages(Runnable loadImages) {
		this.loadImages = loadImages;
	}

	public void showEmail(ParsedEmail email) {
		banner.setVisible(email != null && email.getBlockedImages() > 0);
		if (email == null || Texts.isBlank(email.getPreparedHtml())) {
			showFallback(email, InboxText.get("reader.nohtml"));
			return;
		}
		try {
			HtmlRenderer.show(htmlPanel, agent, email);
			((java.awt.CardLayout) cards.getLayout()).show(cards, "html");
			SwingUtilities.invokeLater(this::reflowHtml);
		} catch (Exception e) {
			LOGGER.warn("HTML rendering failed", e);
			showFallback(email, InboxText.get("reader.render.failed"));
		}
	}

	private void reflowHtml() {
		Dimension viewport = htmlScroll.getViewport().getExtentSize();
		int viewportWidth = viewport.width;
		int viewportHeight = viewport.height;
		if (viewportWidth <= 0) {
			return;
		}
		htmlPanel.setSize(new Dimension(viewportWidth, Math.max(200, viewportHeight)));
		htmlPanel.doLayout();
		Dimension preferred = htmlPanel.getPreferredSize();
		int width = Math.max(viewportWidth, preferred.width);
		int height = Math.max(viewportHeight, preferred.height);
		htmlPanel.setPreferredSize(new Dimension(width, height));
		htmlPanel.revalidate();
		htmlScroll.revalidate();
		htmlScroll.repaint();
	}

	private void showFallback(ParsedEmail email, String note) {
		String body = "";
		if (email != null && !Texts.isBlank(email.getPlainText())) {
			body = email.getPlainText();
		} else if (email != null && email.getRaw() != null) {
			body = email.getRaw();
		}
		fallback.setText(note + "\n\n" + body);
		fallback.setCaretPosition(0);
		((java.awt.CardLayout) cards.getLayout()).show(cards, "text");
	}
}
