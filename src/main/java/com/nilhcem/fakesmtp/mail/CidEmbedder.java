package com.nilhcem.fakesmtp.mail;

import com.nilhcem.fakesmtp.core.Texts;

import java.util.Base64;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rewrites {@code cid:} image references to data URLs so Flying Saucer can paint them.
 * <p>
 * The Swing HTML engine loads {@code <img src>} through {@code ImageResourceLoader}, which
 * opens URIs directly and does not consult {@link MailUserAgent} for non-data addresses.
 * </p>
 *
 * @since 2.1
 */
public final class CidEmbedder {

	private static final Pattern CID_SRC = Pattern.compile(
			"(?i)(\\bsrc\\s*=\\s*[\"'])cid:([^\"']+)([\"'])");

	private CidEmbedder() {
		throw new UnsupportedOperationException();
	}

	public static String embed(String html, Map<String, byte[]> inlineImages) {
		if (Texts.isBlank(html) || inlineImages == null || inlineImages.isEmpty()) {
			return html;
		}
		Matcher matcher = CID_SRC.matcher(html);
		StringBuffer output = new StringBuffer(html.length() + 64);
		while (matcher.find()) {
			String cid = normalize(matcher.group(2));
			byte[] data = lookup(inlineImages, cid);
			if (data == null || data.length == 0) {
				matcher.appendReplacement(output, Matcher.quoteReplacement(matcher.group(0)));
				continue;
			}
			String dataUrl = "data:" + ImageBytes.mimeType(data) + ";base64,"
					+ Base64.getEncoder().encodeToString(data);
			matcher.appendReplacement(output,
					Matcher.quoteReplacement(matcher.group(1) + dataUrl + matcher.group(3)));
		}
		matcher.appendTail(output);
		return output.toString();
	}

	private static byte[] lookup(Map<String, byte[]> images, String cid) {
		byte[] data = images.get(cid);
		if (data != null) {
			return data;
		}
		for (Map.Entry<String, byte[]> entry : images.entrySet()) {
			if (normalize(entry.getKey()).equalsIgnoreCase(cid)) {
				return entry.getValue();
			}
		}
		return null;
	}

	private static String normalize(String value) {
		if (value == null) {
			return "";
		}
		String cid = value.trim();
		if (cid.regionMatches(true, 0, "cid:", 0, 4)) {
			cid = cid.substring(4);
		}
		if (cid.startsWith("<") && cid.endsWith(">") && cid.length() > 2) {
			cid = cid.substring(1, cid.length() - 1);
		}
		return cid.trim();
	}
}
