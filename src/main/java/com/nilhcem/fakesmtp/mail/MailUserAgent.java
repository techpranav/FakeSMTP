package com.nilhcem.fakesmtp.mail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhtmlrenderer.swing.NaiveUserAgent;

/**
 * Supplies only the images that belong to the message being displayed.
 * <p>
 * Network and local-file addresses are refused here. Remote images, when the user allows them,
 * are already downloaded and rewritten to data URLs before rendering.
 * </p>
 *
 * @since 2.1
 */
public final class MailUserAgent extends NaiveUserAgent {

	private static final Logger LOGGER = LoggerFactory.getLogger(MailUserAgent.class);
	private Map<String, byte[]> inlineImages = new HashMap<String, byte[]>();

	public void setInlineImages(Map<String, byte[]> images) {
		inlineImages = new HashMap<String, byte[]>();
		if (images == null) {
			return;
		}
		for (Map.Entry<String, byte[]> entry : images.entrySet()) {
			inlineImages.put(normalizeCid(entry.getKey()), entry.getValue());
		}
	}

	public Map<String, byte[]> getInlineImages() {
		return Collections.unmodifiableMap(inlineImages);
	}

	@Override
	protected InputStream resolveAndOpenStream(String uri) {
		if (uri == null || uri.isBlank()) {
			return null;
		}
		String value = uri.trim();
		if (value.regionMatches(true, 0, "cid:", 0, 4)) {
			byte[] data = inlineImages.get(normalizeCid(value.substring(4)));
			if (data == null) {
				LOGGER.debug("No inline image for {}", value);
				return null;
			}
			return new ByteArrayInputStream(data);
		}
		if (value.regionMatches(true, 0, "data:", 0, 5)) {
			return dataUri(value);
		}
		LOGGER.debug("Blocked a resource outside the email: {}", value);
		return null;
	}

	@Override
	public String resolveURI(String uri) {
		if (uri == null) {
			return "";
		}
		return uri;
	}

	private static InputStream dataUri(String uri) {
		int comma = uri.indexOf(',');
		if (comma < 0) {
			return null;
		}
		String meta = uri.substring(0, comma).toLowerCase(Locale.ROOT);
		String payload = uri.substring(comma + 1);
		try {
			byte[] data;
			if (meta.contains(";base64")) {
				data = Base64.getMimeDecoder().decode(payload);
			} else {
				data = URLDecoder.decode(payload, StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8);
			}
			return new ByteArrayInputStream(data);
		} catch (IllegalArgumentException e) {
			LOGGER.debug("Could not read a data URI image", e);
			return null;
		}
	}

	private static String normalizeCid(String value) {
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
