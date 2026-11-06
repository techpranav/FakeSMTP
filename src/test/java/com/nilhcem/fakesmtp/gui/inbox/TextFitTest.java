package com.nilhcem.fakesmtp.gui.inbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.image.BufferedImage;

import org.junit.Test;

public class TextFitTest {

	@Test
	public void ellipsizeKeepsShortText() {
		FontMetrics metrics = metrics();
		assertEquals("Ada Lovelace", TextFit.ellipsize("Ada Lovelace", metrics, 400));
	}

	@Test
	public void ellipsizeTruncatesLongText() {
		FontMetrics metrics = metrics();
		String longName = "Ada Lovelace <ada.lovelace+engineering@example.invalid>";
		String fitted = TextFit.ellipsize(longName, metrics, 80);
		assertTrue(fitted.endsWith("\u2026"));
		assertTrue(fitted.length() < longName.length());
		assertTrue(metrics.stringWidth(fitted) <= 80);
	}

	@Test
	public void ellipsizeCharsUsesLimit() {
		assertEquals("abcdefghij\u2026", TextFit.ellipsizeChars("abcdefghijklmnopqrstuvwxyz", 11));
		assertEquals("short", TextFit.ellipsizeChars("short", 20));
	}

	private static FontMetrics metrics() {
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
		return image.getGraphics().getFontMetrics(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
	}
}
