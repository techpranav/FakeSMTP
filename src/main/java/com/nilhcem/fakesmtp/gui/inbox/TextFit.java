package com.nilhcem.fakesmtp.gui.inbox;

import java.awt.FontMetrics;

/**
 * Width-aware text truncation for list rows and attachment names.
 *
 * @since 2.1
 */
public final class TextFit {

	private TextFit() {
		throw new UnsupportedOperationException();
	}

	public static String ellipsize(String value, FontMetrics metrics, int maxWidth) {
		if (value == null || value.isEmpty() || maxWidth <= 0) {
			return "";
		}
		if (metrics == null || metrics.stringWidth(value) <= maxWidth) {
			return value;
		}
		String ellipsis = "\u2026";
		int ellipsisWidth = metrics.stringWidth(ellipsis);
		if (ellipsisWidth >= maxWidth) {
			return ellipsis;
		}
		int end = value.length();
		while (end > 0 && metrics.stringWidth(value.substring(0, end)) + ellipsisWidth > maxWidth) {
			end--;
		}
		return value.substring(0, end) + ellipsis;
	}

	public static String ellipsizeChars(String value, int maxChars) {
		if (value == null) {
			return "";
		}
		if (maxChars <= 1 || value.length() <= maxChars) {
			return value;
		}
		return value.substring(0, maxChars - 1) + "\u2026";
	}
}
