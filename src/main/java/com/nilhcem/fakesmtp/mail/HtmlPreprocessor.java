package com.nilhcem.fakesmtp.mail;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Turns untrusted email HTML into a document the renderer can show safely.
 * <p>
 * Scripts and active content are removed. Remote images stay unloaded unless the caller
 * explicitly asks for them, and even then only HTTP and HTTPS image responses are accepted.
 * SVG drawings are converted to PNG images because the HTML renderer does not draw SVG.
 * </p>
 *
 * @since 2.1
 */
public final class HtmlPreprocessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(HtmlPreprocessor.class);
	private static final int CONNECT_TIMEOUT_MS = 4000;
	private static final int READ_TIMEOUT_MS = 6000;
	private static final int MAX_IMAGE_BYTES = 2_000_000;
	private static final String[] REMOVED_TAGS = {
		"script", "iframe", "object", "embed", "applet", "link", "meta", "base"
	};
	private static final String DOCUMENT_STYLE = "body{margin:0;padding:16px;background:#ffffff;color:#000000;"
			+ "font-family:Calibri,Arial,Helvetica,sans-serif;font-size:12pt;line-height:1.35;}"
			+ "a{color:#0563C1;text-decoration:underline;}"
			+ "table{border-collapse:collapse;border-spacing:0;max-width:100%;}"
			+ "th,td{vertical-align:top;}"
			+ "img{max-width:100%;height:auto;border:0;}"
			+ "p{margin:0 0 8px 0;}"
			+ "ul,ol{margin:0 0 8px 24px;padding:0;}"
			+ "blockquote{margin:8px 0;padding-left:12px;border-left:3px solid #c7c7c7;color:#333333;}"
			+ "hr{border:0;border-top:1px solid #c7c7c7;margin:12px 0;}"
			+ "pre,code{font-family:Consolas,monospace;font-size:11pt;}"
			+ "form{margin:0;padding:0;}";

	private HtmlPreprocessor() {
		throw new UnsupportedOperationException();
	}

	public static PreparedHtml prepare(String html, boolean loadRemoteImages) {
		Document document = Jsoup.parse(html == null ? "" : html);
		document.outputSettings()
				.syntax(Document.OutputSettings.Syntax.xml)
				.escapeMode(Entities.EscapeMode.xhtml)
				.charset(StandardCharsets.UTF_8)
				.prettyPrint(false);
		for (String tag : REMOVED_TAGS) {
			document.select(tag).remove();
		}
		stripActiveContent(document);
		disableForms(document);
		replaceSvg(document);
		int blocked = rewriteImages(document, loadRemoteImages);
		Element style = document.createElement("style");
		style.appendChild(new DataNode(DOCUMENT_STYLE));
		document.head().appendChild(style);
		return new PreparedHtml(document.outerHtml(), blocked);
	}

	private static void stripActiveContent(Document document) {
		for (Element element : document.getAllElements()) {
			List<String> drop = new ArrayList<String>();
			element.attributes().forEach(attribute -> {
				String key = attribute.getKey();
				if (key != null && key.toLowerCase(Locale.ROOT).startsWith("on")) {
					drop.add(key);
				}
			});
			for (String key : drop) {
				element.removeAttr(key);
			}
			neutralizeUrl(element, "href");
			neutralizeUrl(element, "src");
		}
	}

	private static void neutralizeUrl(Element element, String attribute) {
		String value = element.attr(attribute).trim().toLowerCase(Locale.ROOT);
		if (value.startsWith("javascript:") || value.startsWith("vbscript:") || value.startsWith("data:text/html")) {
			element.removeAttr(attribute);
		}
	}

	private static void disableForms(Document document) {
		for (Element form : document.select("form")) {
			form.removeAttr("action");
			form.removeAttr("method");
			form.removeAttr("target");
		}
		for (Element control : document.select("input, button, select, textarea")) {
			control.attr("disabled", "disabled");
		}
	}

	private static void replaceSvg(Document document) {
		for (Element svg : document.select("svg")) {
			if (!svg.hasAttr("xmlns")) {
				svg.attr("xmlns", "http://www.w3.org/2000/svg");
			}
			byte[] png = SvgRasterizer.toPng(svg.outerHtml());
			if (png == null) {
				svg.replaceWith(fallback(document, svg.text()));
				continue;
			}
			Element image = document.createElement("img");
			image.attr("src", "data:image/png;base64," + Base64.getEncoder().encodeToString(png));
			image.attr("alt", "Embedded chart");
			copySize(svg, image, "width");
			copySize(svg, image, "height");
			svg.replaceWith(image);
		}
	}

	private static Element fallback(Document document, String text) {
		Element paragraph = document.createElement("p");
		if (text == null || text.isBlank()) {
			paragraph.text("An embedded drawing could not be shown.");
		} else {
			paragraph.text(text);
		}
		return paragraph;
	}

	private static void copySize(Element source, Element target, String name) {
		String value = source.attr(name).trim();
		if (!value.isEmpty()) {
			target.attr(name, value);
		}
	}

	private static int rewriteImages(Document document, boolean loadRemoteImages) {
		int blocked = 0;
		for (Element image : document.select("img[src]")) {
			String src = image.attr("src").trim();
			String lower = src.toLowerCase(Locale.ROOT);
			if (lower.startsWith("file:") || lower.startsWith("jar:") || lower.startsWith("javascript:")) {
				block(image);
				blocked++;
				continue;
			}
			if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
				continue;
			}
			if (!loadRemoteImages) {
				block(image);
				blocked++;
				continue;
			}
			byte[] data = fetchImage(src);
			if (data == null) {
				block(image);
				blocked++;
				continue;
			}
			image.attr("src", "data:image/png;base64," + Base64.getEncoder().encodeToString(data));
		}
		return blocked;
	}

	private static void block(Element image) {
		image.removeAttr("src");
		if (image.attr("alt").isBlank()) {
			image.attr("alt", "Blocked image");
		}
	}

	private static byte[] fetchImage(String url) {
		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
			connection.setInstanceFollowRedirects(false);
			connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
			connection.setReadTimeout(READ_TIMEOUT_MS);
			connection.setRequestProperty("User-Agent", "FakeSMTP");
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				return null;
			}
			String type = connection.getContentType();
			if (type == null || !type.toLowerCase(Locale.ROOT).startsWith("image/")) {
				return null;
			}
			try (InputStream input = connection.getInputStream()) {
				return readLimited(input);
			}
		} catch (Exception e) {
			LOGGER.debug("Could not load a remote image for an email", e);
			return null;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	private static byte[] readLimited(InputStream input) throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		int total = 0;
		int read;
		while ((read = input.read(buffer)) >= 0) {
			total += read;
			if (total > MAX_IMAGE_BYTES) {
				return null;
			}
			output.write(buffer, 0, read);
		}
		return output.toByteArray();
	}
}
