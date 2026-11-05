package com.nilhcem.fakesmtp.mail;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.nilhcem.fakesmtp.core.Texts;

/**
 * Prepares HTML for Outlook {@code .msg} export.
 * <p>
 * Outlook does not render SVG and is unreliable with large {@code data:} URLs, so this
 * normalizer turns SVG drawings and embedded {@code data:image…} payloads into
 * {@code cid:} parts that {@link OutlookMsgWriter} attaches with MHTML reference flags.
 * Existing {@code cid:} references are left intact for MIME inline parts already parsed.
 * </p>
 *
 * @since 2.1
 */
public final class OutlookHtmlNormalizer {

	private static final Pattern DATA_URL_IN_CSS = Pattern.compile(
			"(?i)(url\\(\\s*['\"]?)(data:image[^)'\"]+)(['\"]?\\s*\\))");

	private OutlookHtmlNormalizer() {
		throw new UnsupportedOperationException();
	}

	public static Result normalize(String html) {
		if (Texts.isBlank(html)) {
			return new Result("", new LinkedHashMap<String, byte[]>());
		}
		Document document = Jsoup.parse(html);
		document.outputSettings().prettyPrint(false).charset("UTF-8");
		if (document.head() != null && document.head().select("meta[charset]").isEmpty()) {
			document.head().prependElement("meta").attr("charset", "UTF-8");
		}
		Map<String, byte[]> inline = new LinkedHashMap<String, byte[]>();
		Map<String, String> mimeByCid = new LinkedHashMap<String, String>();
		int index = 0;
		index = replaceSvgs(document, inline, mimeByCid, index);
		index = replaceDataImages(document, inline, mimeByCid, index);
		rewriteDataUrlsInStyles(document, inline, mimeByCid, index);

		String bodyHtml = document.body() != null ? document.body().html() : document.html();
		String full = "<html><head><meta charset=\"UTF-8\"/></head><body>" + bodyHtml + "</body></html>";
		return new Result(full, inline, mimeByCid);
	}

	private static int replaceSvgs(Document document, Map<String, byte[]> inline,
			Map<String, String> mimeByCid, int index) {
		for (Element svg : document.select("svg")) {
			if (!svg.hasAttr("xmlns")) {
				svg.attr("xmlns", "http://www.w3.org/2000/svg");
			}
			byte[] png = SvgRasterizer.toPng(svg.outerHtml());
			if (png == null || png.length == 0) {
				Element fallback = document.createElement("p");
				String text = svg.text();
				fallback.text(Texts.isBlank(text) ? "Chart could not be rendered." : text);
				svg.replaceWith(fallback);
				continue;
			}
			String cid = nextCid("chart", index++);
			inline.put(cid, png);
			mimeByCid.put(cid, "image/png");
			Element image = document.createElement("img");
			image.attr("src", "cid:" + cid);
			image.attr("alt", "Chart");
			copySize(svg, image, "width");
			copySize(svg, image, "height");
			image.attr("style", "display:block;border:0;max-width:100%;");
			svg.replaceWith(image);
		}
		return index;
	}

	private static int replaceDataImages(Document document, Map<String, byte[]> inline,
			Map<String, String> mimeByCid, int index) {
		for (Element image : document.select("img[src]")) {
			String src = image.attr("src").trim();
			if (!src.regionMatches(true, 0, "data:image", 0, 10)) {
				continue;
			}
			ImageBytes.DecodedDataUri decoded = ImageBytes.decodeDataUri(src);
			if (decoded == null) {
				image.attr("alt", Texts.isBlank(image.attr("alt")) ? "Embedded image unavailable" : image.attr("alt"));
				image.removeAttr("src");
				continue;
			}
			String cid = nextCid("embed", index++);
			inline.put(cid, decoded.getData());
			mimeByCid.put(cid, decoded.getMimeType());
			image.attr("src", "cid:" + cid);
		}
		return index;
	}

	private static int rewriteDataUrlsInStyles(Document document, Map<String, byte[]> inline,
			Map<String, String> mimeByCid, int index) {
		int next = index;
		for (Element element : document.getAllElements()) {
			String style = element.attr("style");
			if (Texts.isBlank(style) || !style.toLowerCase(Locale.ROOT).contains("data:image")) {
				continue;
			}
			Matcher matcher = DATA_URL_IN_CSS.matcher(style);
			StringBuffer rewritten = new StringBuffer(style.length());
			boolean changed = false;
			while (matcher.find()) {
				ImageBytes.DecodedDataUri decoded = ImageBytes.decodeDataUri(matcher.group(2));
				if (decoded == null) {
					matcher.appendReplacement(rewritten, Matcher.quoteReplacement(matcher.group(0)));
					continue;
				}
				String cid = nextCid("bg", next++);
				inline.put(cid, decoded.getData());
				mimeByCid.put(cid, decoded.getMimeType());
				matcher.appendReplacement(rewritten,
						Matcher.quoteReplacement(matcher.group(1) + "cid:" + cid + matcher.group(3)));
				changed = true;
			}
			matcher.appendTail(rewritten);
			if (changed) {
				element.attr("style", rewritten.toString());
			}
		}
		return next;
	}

	private static String nextCid(String kind, int index) {
		return "mailbox-" + kind + "-" + index + "@mailbox.local";
	}

	private static void copySize(Element source, Element target, String name) {
		String value = source.attr(name).trim();
		if (!value.isEmpty()) {
			target.attr(name, value);
		}
	}

	public static final class Result {
		private final String html;
		private final Map<String, byte[]> inlineImages;
		private final Map<String, String> mimeByCid;

		Result(String html, Map<String, byte[]> inlineImages) {
			this(html, inlineImages, new LinkedHashMap<String, String>());
		}

		Result(String html, Map<String, byte[]> inlineImages, Map<String, String> mimeByCid) {
			this.html = html;
			this.inlineImages = inlineImages;
			this.mimeByCid = mimeByCid;
		}

		public String getHtml() {
			return html;
		}

		public Map<String, byte[]> getInlineImages() {
			return inlineImages;
		}

		public String mimeFor(String cid) {
			String mime = mimeByCid.get(cid);
			return Texts.isBlank(mime) ? null : mime;
		}

		public List<Map.Entry<String, byte[]>> inlineEntries() {
			return new ArrayList<Map.Entry<String, byte[]>>(inlineImages.entrySet());
		}
	}
}
