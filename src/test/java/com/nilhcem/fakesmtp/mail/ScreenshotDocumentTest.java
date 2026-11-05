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
				.html("<html><body><p>Hello body</p><table><tr><td>E00827</td></tr></table></body></html>")
				.readable(true)
				.build();
		java.awt.image.BufferedImage image = EmailScreenshot.capture(email);
		assertTrue(image.getWidth() >= 900);
		assertTrue(image.getHeight() > 80);
		assertTrue(ScreenshotDocument.compose(email).contains("Overdue approval"));
		assertFalse(ScreenshotDocument.compose(email).toLowerCase().contains("mailbox-theme"));
	}
}
