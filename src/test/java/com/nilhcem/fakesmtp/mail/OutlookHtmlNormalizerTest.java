package com.nilhcem.fakesmtp.mail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Base64;

import org.junit.Test;

public class OutlookHtmlNormalizerTest {

	private static final byte[] TINY_PNG = Base64.getDecoder().decode(
			"iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

	@Test
	public void rasterizesOverdueSvgsToCidImages() {
		String raw = TestEmailFactory.buildOverdueRaw();
		ParsedEmail parsed = EmailParser.parse(raw, "Enovia@technia.com",
				"KTApprove02@kla-tencor.com", new java.util.Date(), null, false);
		OutlookHtmlNormalizer.Result result = OutlookHtmlNormalizer.normalize(parsed.getHtml());
		assertFalse(result.getHtml().toLowerCase().contains("<svg"));
		assertTrue(result.getHtml().contains("cid:mailbox-chart-"));
		assertEquals(2, result.getInlineImages().size());
		for (byte[] png : result.getInlineImages().values()) {
			assertTrue(png.length > 500);
			assertEquals((byte) 0x89, png[0]);
		}
	}

	@Test
	public void convertsDataUriImagesToCid() {
		String html = "<html><body><p>Logo</p>"
				+ "<img src=\"data:image/png;base64,"
				+ Base64.getEncoder().encodeToString(TINY_PNG)
				+ "\" alt=\"logo\"/>"
				+ "<div style=\"background-image:url(data:image/png;base64,"
				+ Base64.getEncoder().encodeToString(TINY_PNG)
				+ ")\">cell</div></body></html>";
		OutlookHtmlNormalizer.Result result = OutlookHtmlNormalizer.normalize(html);
		assertFalse(result.getHtml().toLowerCase().contains("data:image"));
		assertTrue(result.getHtml().contains("cid:mailbox-embed-"));
		assertTrue(result.getHtml().contains("cid:mailbox-bg-"));
		assertTrue(result.getInlineImages().size() >= 2);
		assertEquals("image/png", result.mimeFor(result.getInlineImages().keySet().iterator().next()));
	}

	@Test
	public void imageBytesSniffsOfficeAndPng() {
		assertEquals("image/png", ImageBytes.mimeType(TINY_PNG));
		assertEquals(".png", ImageBytes.extension("image/png"));
		assertEquals(".xlsx", ImageBytes.extension("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
		assertNotNull(ImageBytes.decodeDataUri("data:image/png;base64,"
				+ Base64.getEncoder().encodeToString(TINY_PNG)));
	}
}
