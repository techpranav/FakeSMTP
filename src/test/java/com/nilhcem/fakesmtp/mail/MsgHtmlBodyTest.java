package com.nilhcem.fakesmtp.mail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.poi.hmef.CompressedRTF;
import org.junit.Test;

public class MsgHtmlBodyTest {

	@Test
	public void buildsBinaryHtmlAndRoundTripsUncompressedRtf() throws Exception {
		String html = "<html><body><table><tr><td>E00827</td></tr></table></body></html>";
		byte[] binary = MsgHtmlBody.htmlBinary(html);
		assertTrue(binary.length > 20);
		assertEquals(html, new String(binary, java.nio.charset.StandardCharsets.UTF_8));

		byte[] compressed = MsgHtmlBody.rtfCompressedFromHtml(html);
		CompressedRTF decoder = new CompressedRTF();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		decoder.decompress(new ByteArrayInputStream(compressed), out);
		String rtf = out.toString("UTF-8");
		assertTrue(rtf.contains("\\fromhtml1"));
		assertTrue(rtf.contains("E00827"));
	}
}
