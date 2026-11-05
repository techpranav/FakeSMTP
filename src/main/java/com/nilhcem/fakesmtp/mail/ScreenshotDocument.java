package com.nilhcem.fakesmtp.mail;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import com.nilhcem.fakesmtp.core.Texts;

/**
 * Builds a self-contained HTML page for full-email PNG capture: metadata + original body.
 * Does not mutate the stored message HTML.
 *
 * @since 2.1
 */
public final class ScreenshotDocument {

	private ScreenshotDocument() {
		throw new UnsupportedOperationException();
	}

	public static String compose(ParsedEmail parsed) {
		StringBuilder html = new StringBuilder();
		html.append("<html><head><meta charset=\"UTF-8\"/></head>");
		html.append("<body style=\"margin:0;padding:0;background:#ffffff;color:#202124\">");
		html.append("<div style=\"font-family:'Segoe UI',Arial,sans-serif;padding:20px 24px 16px 24px;");
		html.append("border-bottom:1px solid #dadce0;background:#f8f9fb\">");
		html.append("<div style=\"font-size:22px;font-weight:600;margin:0 0 14px 0;line-height:1.3\">");
		html.append(escape(blank(parsed.getSubject(), "(no subject)")));
		html.append("</div>");
		appendField(html, "From", formatList(parsed.getFrom()));
		appendField(html, "To", formatList(parsed.getTo()));
		if (hasAddresses(parsed.getCc())) {
			appendField(html, "CC", formatList(parsed.getCc()));
		}
		if (hasAddresses(parsed.getBcc())) {
			appendField(html, "BCC", formatList(parsed.getBcc()));
		}
		if (parsed.getReceivedDate() != null) {
			String date = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US)
					.format(parsed.getReceivedDate());
			appendField(html, "Date", date);
		}
		html.append("</div>");
		html.append("<div style=\"padding:16px 24px 24px 24px;background:#ffffff\">");
		html.append(bodyFragment(parsed));
		html.append("</div></body></html>");
		return html.toString();
	}

	private static String bodyFragment(ParsedEmail parsed) {
		if (!Texts.isBlank(parsed.getPreparedHtml())) {
			return extractInner(parsed.getPreparedHtml());
		}
		if (!Texts.isBlank(parsed.getHtml())) {
			return extractInner(parsed.getHtml());
		}
		if (!Texts.isBlank(parsed.getPlainText())) {
			return "<pre style=\"font-family:Consolas,'Courier New',monospace;white-space:pre-wrap;margin:0\">"
					+ escape(parsed.getPlainText()) + "</pre>";
		}
		return "<p style=\"color:#5f6368\">(no body)</p>";
	}

	private static String extractInner(String html) {
		String lower = html.toLowerCase(Locale.ROOT);
		int bodyStart = lower.indexOf("<body");
		if (bodyStart >= 0) {
			int gt = html.indexOf('>', bodyStart);
			int bodyEnd = lower.lastIndexOf("</body>");
			if (gt > 0 && bodyEnd > gt) {
				return html.substring(gt + 1, bodyEnd);
			}
		}
		return html;
	}

	private static void appendField(StringBuilder html, String label, String value) {
		if (Texts.isBlank(value)) {
			return;
		}
		html.append("<div style=\"font-size:12.5px;margin:0 0 6px 0;line-height:1.45\">");
		html.append("<span style=\"color:#5f6368;font-weight:600\">").append(escape(label)).append(":</span> ");
		html.append("<span style=\"color:#202124\">").append(escape(value)).append("</span></div>");
	}

	private static boolean hasAddresses(List<EmailAddress> addresses) {
		return !Texts.isBlank(formatList(addresses));
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

	private static String escape(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
