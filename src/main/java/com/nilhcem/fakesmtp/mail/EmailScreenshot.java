package com.nilhcem.fakesmtp.mail;

import java.awt.image.BufferedImage;

/**
 * Builds a complete email PNG: metadata header (From/To/CC/BCC/Date) + full HTML/plain body.
 *
 * @since 2.1
 */
public final class EmailScreenshot {

	private static final int WIDTH = 960;
	private static final int MAX_BODY_HEIGHT = 30000;

	private EmailScreenshot() {
		throw new UnsupportedOperationException();
	}

	public static BufferedImage capture(ParsedEmail parsed) {
		if (parsed == null) {
			throw new IllegalArgumentException("No message to capture");
		}
		ensurePrepared(parsed);
		ParsedEmail document = ScreenshotDocument.asCaptureEmail(parsed);
		return HtmlRenderer.renderFull(document, WIDTH, MAX_BODY_HEIGHT);
	}

	private static void ensurePrepared(ParsedEmail parsed) {
		if (parsed.getPreparedHtml() != null && !parsed.getPreparedHtml().isEmpty()) {
			return;
		}
		if (parsed.getHtml() != null && !parsed.getHtml().isEmpty()) {
			parsed.applyPrepared(HtmlPreprocessor.prepare(parsed.getHtml(), false));
		}
	}
}
