package com.nilhcem.fakesmtp.mail;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.nilhcem.fakesmtp.core.Texts;

/**
 * Builds a complete email PNG: metadata header + full off-screen HTML/plain body.
 *
 * @since 2.1
 */
public final class EmailScreenshot {

	private static final int PAD = 24;
	private static final int WIDTH = 960;
	private static final int MAX_BODY_HEIGHT = 20000;

	private EmailScreenshot() {
		throw new UnsupportedOperationException();
	}

	public static BufferedImage capture(ParsedEmail parsed) {
		if (parsed == null) {
			throw new IllegalArgumentException("No message to capture");
		}
		BufferedImage header = renderHeader(parsed);
		BufferedImage body = renderBody(parsed);
		int height = header.getHeight() + body.getHeight();
		BufferedImage image = new BufferedImage(WIDTH, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		try {
			graphics.setColor(Color.WHITE);
			graphics.fillRect(0, 0, WIDTH, height);
			graphics.drawImage(header, 0, 0, null);
			graphics.drawImage(body, 0, header.getHeight(), null);
		} finally {
			graphics.dispose();
		}
		return image;
	}

	private static BufferedImage renderHeader(ParsedEmail parsed) {
		List<String> lines = new ArrayList<String>();
		lines.add(blank(parsed.getSubject(), "(no subject)"));
		addLine(lines, "From", formatList(parsed.getFrom()));
		addLine(lines, "To", formatList(parsed.getTo()));
		addLine(lines, "CC", formatList(parsed.getCc()));
		addLine(lines, "BCC", formatList(parsed.getBcc()));
		if (parsed.getReceivedDate() != null) {
			addLine(lines, "Date", new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US)
					.format(parsed.getReceivedDate()));
		}
		int height = PAD * 2 + 36 + Math.max(0, lines.size() - 1) * 22 + 12;
		BufferedImage image = new BufferedImage(WIDTH, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		try {
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setColor(new Color(0xF8F9FB));
			g.fillRect(0, 0, WIDTH, height);
			g.setColor(new Color(0xDADCE0));
			g.drawLine(0, height - 1, WIDTH, height - 1);
			int y = PAD + 24;
			g.setFont(new Font("SansSerif", Font.BOLD, 20));
			g.setColor(new Color(0x202124));
			g.drawString(clip(lines.get(0), 90), PAD, y);
			g.setFont(new Font("SansSerif", Font.PLAIN, 13));
			g.setColor(new Color(0x3C4043));
			for (int i = 1; i < lines.size(); i++) {
				y += 22;
				g.drawString(clip(lines.get(i), 110), PAD, y);
			}
		} finally {
			g.dispose();
		}
		return image;
	}

	private static BufferedImage renderBody(ParsedEmail parsed) {
		ensurePrepared(parsed);
		if (!Texts.isBlank(parsed.getPreparedHtml()) || !Texts.isBlank(parsed.getHtml())) {
			return HtmlRenderer.renderFull(parsed, WIDTH, MAX_BODY_HEIGHT);
		}
		String text = !Texts.isBlank(parsed.getPlainText()) ? parsed.getPlainText() : "(no body)";
		String[] rows = text.split("\\R", -1);
		int height = Math.max(120, PAD * 2 + rows.length * 18);
		if (height > MAX_BODY_HEIGHT) {
			throw new IllegalArgumentException("Email content height " + height
					+ "px exceeds the capture limit of " + MAX_BODY_HEIGHT + "px");
		}
		BufferedImage image = new BufferedImage(WIDTH, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		try {
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, WIDTH, height);
			g.setColor(new Color(0x202124));
			g.setFont(new Font("Monospaced", Font.PLAIN, 12));
			int y = PAD + 12;
			for (String row : rows) {
				g.drawString(clip(row, 120), PAD, y);
				y += 18;
			}
		} finally {
			g.dispose();
		}
		return image;
	}

	private static void ensurePrepared(ParsedEmail parsed) {
		if (!Texts.isBlank(parsed.getPreparedHtml())) {
			return;
		}
		if (!Texts.isBlank(parsed.getHtml())) {
			parsed.applyPrepared(HtmlPreprocessor.prepare(parsed.getHtml(), false));
		}
	}

	private static void addLine(List<String> lines, String label, String value) {
		if (!Texts.isBlank(value)) {
			lines.add(label + ": " + value);
		}
	}

	private static String formatList(List<EmailAddress> addresses) {
		if (addresses == null || addresses.isEmpty()) {
			return "";
		}
		StringBuilder out = new StringBuilder();
		for (EmailAddress address : addresses) {
			if (address == null) {
				continue;
			}
			String line = address.format();
			if (Texts.isBlank(line)) {
				continue;
			}
			if (out.length() > 0) {
				out.append("; ");
			}
			out.append(line);
		}
		return out.toString();
	}

	private static String blank(String value, String fallback) {
		return Texts.isBlank(value) ? fallback : value;
	}

	private static String clip(String value, int max) {
		if (value == null) {
			return "";
		}
		if (value.length() <= max) {
			return value;
		}
		return value.substring(0, max - 1) + "\u2026";
	}
}
