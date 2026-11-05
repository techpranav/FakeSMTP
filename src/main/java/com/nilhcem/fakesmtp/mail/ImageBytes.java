package com.nilhcem.fakesmtp.mail;

import java.util.Base64;
import java.util.Locale;

import com.nilhcem.fakesmtp.core.Texts;

/**
 * Detects image MIME types and decodes {@code data:} image URLs for MSG / HTML embedding.
 *
 * @since 2.1
 */
public final class ImageBytes {

	private ImageBytes() {
		throw new UnsupportedOperationException();
	}

	public static String mimeType(byte[] data) {
		if (data == null || data.length == 0) {
			return "application/octet-stream";
		}
		if (data.length >= 3 && (data[0] & 0xff) == 0xff && (data[1] & 0xff) == 0xd8 && (data[2] & 0xff) == 0xff) {
			return "image/jpeg";
		}
		if (data.length >= 8 && data[0] == (byte) 0x89 && data[1] == 0x50 && data[2] == 0x4e && data[3] == 0x47) {
			return "image/png";
		}
		if (data.length >= 6) {
			String header = new String(data, 0, 6, java.nio.charset.StandardCharsets.US_ASCII)
					.toLowerCase(Locale.ROOT);
			if (header.startsWith("gif87a") || header.startsWith("gif89a")) {
				return "image/gif";
			}
		}
		if (data.length >= 12
				&& data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F'
				&& data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P') {
			return "image/webp";
		}
		if (data.length >= 2 && data[0] == (byte) 0x42 && data[1] == (byte) 0x4d) {
			return "image/bmp";
		}
		// ZIP-based Office Open XML (xlsx/docx/pptx) or plain ZIP
		if (data.length >= 4 && data[0] == 0x50 && data[1] == 0x4b && data[2] == 0x03 && data[3] == 0x04) {
			return "application/zip";
		}
		// OLE Compound Document (legacy .xls / .doc / .ppt)
		if (data.length >= 8
				&& (data[0] & 0xff) == 0xd0 && (data[1] & 0xff) == 0xcf
				&& (data[2] & 0xff) == 0x11 && (data[3] & 0xff) == 0xe0) {
			return "application/vnd.ms-office";
		}
		return "application/octet-stream";
	}

	public static String extension(String mimeType) {
		String mime = Texts.isBlank(mimeType) ? "" : mimeType.toLowerCase(Locale.ROOT);
		if (mime.contains("jpeg") || mime.contains("jpg")) {
			return ".jpg";
		}
		if (mime.contains("png")) {
			return ".png";
		}
		if (mime.contains("gif")) {
			return ".gif";
		}
		if (mime.contains("webp")) {
			return ".webp";
		}
		if (mime.contains("bmp")) {
			return ".bmp";
		}
		if (mime.contains("sheet") || mime.contains("excel") || mime.endsWith("xls")) {
			return mime.contains("openxml") ? ".xlsx" : ".xls";
		}
		if (mime.contains("wordprocessing") || mime.contains("msword")) {
			return mime.contains("openxml") ? ".docx" : ".doc";
		}
		if (mime.contains("presentation") || mime.contains("powerpoint")) {
			return mime.contains("openxml") ? ".pptx" : ".ppt";
		}
		if (mime.contains("pdf")) {
			return ".pdf";
		}
		if (mime.contains("zip") || mime.contains("octet-stream")) {
			return ".bin";
		}
		if (mime.startsWith("image/")) {
			return ".img";
		}
		return ".bin";
	}

	/**
	 * Decodes {@code data:[mime][;base64],payload}. Returns null when not a usable data URL.
	 */
	public static DecodedDataUri decodeDataUri(String src) {
		if (Texts.isBlank(src)) {
			return null;
		}
		String value = src.trim();
		if (!value.regionMatches(true, 0, "data:", 0, 5)) {
			return null;
		}
		int comma = value.indexOf(',');
		if (comma < 5 || comma >= value.length() - 1) {
			return null;
		}
		String meta = value.substring(5, comma).toLowerCase(Locale.ROOT);
		String payload = value.substring(comma + 1);
		boolean base64 = meta.contains(";base64");
		String mime = meta;
		int semi = meta.indexOf(';');
		if (semi >= 0) {
			mime = meta.substring(0, semi);
		}
		if (Texts.isBlank(mime)) {
			mime = "application/octet-stream";
		}
		byte[] data;
		try {
			if (base64) {
				data = Base64.getDecoder().decode(payload.replaceAll("\\s+", ""));
			} else {
				data = java.net.URLDecoder.decode(payload, "UTF-8")
						.getBytes(java.nio.charset.StandardCharsets.UTF_8);
			}
		} catch (Exception e) {
			return null;
		}
		if (data == null || data.length == 0) {
			return null;
		}
		if ("application/octet-stream".equals(mime) || Texts.isBlank(mime)) {
			mime = mimeType(data);
		}
		return new DecodedDataUri(mime, data);
	}

	public static final class DecodedDataUri {
		private final String mimeType;
		private final byte[] data;

		DecodedDataUri(String mimeType, byte[] data) {
			this.mimeType = mimeType;
			this.data = data;
		}

		public String getMimeType() {
			return mimeType;
		}

		public byte[] getData() {
			return data;
		}
	}
}
