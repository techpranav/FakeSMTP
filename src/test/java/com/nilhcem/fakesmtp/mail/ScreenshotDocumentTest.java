package com.nilhcem.fakesmtp.mail;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Date;

import org.junit.Test;

public class ScreenshotDocumentTest {

	@Test
	public void includesMetadataAndBody() {
		ParsedEmail email = ParsedEmail.builder()
				.subject("Overdue approval")
				.from(Collections.singletonList(new EmailAddress("ENOVIA", "enovia@example.com")))
				.to(Collections.singletonList(new EmailAddress(null, "approver@example.com")))
				.cc(Collections.singletonList(new EmailAddress(null, "cc@example.com")))
				.receivedDate(new Date())
				.html("<html><body><p>Hello body</p><table><tr><td>E00827</td></tr></table>"
						+ "<p>Footer line for full capture</p></body></html>")
				.readable(true)
				.build();
		String page = ScreenshotDocument.compose(email);
		assertTrue(page.contains("Overdue approval"));
		assertTrue(page.contains("From:"));
		assertTrue(page.contains("enovia@example.com"));
		assertTrue(page.contains("To:"));
		assertTrue(page.contains("approver@example.com"));
		assertTrue(page.contains("CC:"));
		assertTrue(page.contains("cc@example.com"));
		assertFalse(page.toLowerCase().contains("mailbox-theme"));
		java.awt.image.BufferedImage image = EmailScreenshot.capture(email);
		assertTrue(image.getWidth() >= 900);
		assertTrue("screenshot should include metadata + body, height=" + image.getHeight(),
				image.getHeight() > 200);
	}

	@Test
	public void sampleEmailScreenshotIncludesAddressesAndFullBody() {
		String raw = TestEmailFactory.buildRaw();
		ParsedEmail email = EmailParser.parse(raw, "tester@mailbox.local", "developer@example.com",
				new Date(), null, false);
		String page = ScreenshotDocument.compose(email);
		assertTrue(page.contains("From:"));
		assertTrue(page.contains("tester@mailbox.local"));
		assertTrue(page.contains("To:"));
		assertTrue(page.contains("developer@example.com"));
		assertTrue(page.contains("CC:"));
		assertTrue(page.contains("qa@example.com"));
		assertTrue(page.contains("Approver 02"));
		assertTrue(page.contains("Assignment distribution"));
		java.awt.image.BufferedImage image = EmailScreenshot.capture(email);
		assertTrue("full sample body should be tall, height=" + image.getHeight(), image.getHeight() > 500);
	}
}
