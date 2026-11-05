package com.nilhcem.fakesmtp.mail;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

import org.apache.poi.hmef.CompressedRTF;

import com.nilhcem.fakesmtp.core.Texts;

/**
 * Builds Outlook-compatible HTML body payloads for {@code .msg} files:
 * binary {@code PidTagHtml} (0x1013/0102) and uncompressed {@code PidTagRtfCompressed}
 * wrapping the HTML with {@code \\fromhtml1} so Outlook renders the HTML view.
 *
 * @since 2.1
 */
public final class MsgHtmlBody {

	private MsgHtmlBody() {
		throw new UnsupportedOperationException();
	}

	/**
	 * UTF-8 bytes for {@code PidTagHtml} / {@code PR_HTML} (binary property 0x1013).
	 */
	public static byte[] htmlBinary(String html) {
		if (Texts.isBlank(html)) {
			return new byte[0];
		}
		return html.getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * Uncompressed RTF-compressed blob (MELA signature) embedding HTML via {@code \\fromhtml1}.
	 * Outlook uses this to recover and display the HTML body.
	 */
	public static byte[] rtfCompressedFromHtml(String html) {
		if (Texts.isBlank(html)) {
			return new byte[0];
		}
		byte[] rtf = buildFromHtmlRtf(html).getBytes(StandardCharsets.UTF_8);
		return wrapUncompressedRtf(rtf);
	}

	static String buildFromHtmlRtf(String html) {
		// Keep HTML largely intact; escape RTF control characters that would break the wrapper.
		String safe = html.replace("\\", "\\\\").replace("{", "\\{").replace("}", "\\}");
		return "{\\rtf1\\ansi\\ansicpg65001\\fromhtml1\\deff0{\\fonttbl{\\f0\\fswiss Helvetica;}}"
				+ "{\\colortbl;\\red0\\green0\\blue0;}\\f0\\fs24 \\htmlrtf0 " + safe + "}";
	}

	/**
	 * MS-OXRTFCP uncompressed layout: CompSize, RawSize, "MELA", CRC32, then raw RTF bytes.
	 */
	static byte[] wrapUncompressedRtf(byte[] rtf) {
		CRC32 crc = new CRC32();
		crc.update(rtf);
		ByteBuffer buffer = ByteBuffer.allocate(16 + rtf.length).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(12 + rtf.length);
		buffer.putInt(rtf.length);
		buffer.putInt(CompressedRTF.UNCOMPRESSED_SIGNATURE_INT);
		buffer.putInt((int) crc.getValue());
		buffer.put(rtf);
		return buffer.array();
	}
}
