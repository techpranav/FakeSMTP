package com.nilhcem.fakesmtp.mail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Date;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.nilhcem.fakesmtp.model.EmailModel;
import com.nilhcem.fakesmtp.model.InboxMessage;

import ch.astorm.jotlmsg.OutlookMessage;

public class EmailExportTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void writesGenuineMsgWithRecipientsAndHtmlAttachment() throws Exception {
		String raw = TestEmailFactory.buildRaw();
		ParsedEmail parsed = EmailParser.parse(raw, "tester@mailbox.local", "developer@example.com", new Date(),
				folder.newFile("sample.eml").getAbsolutePath(), false);
		InboxMessage message = message(parsed, raw);

		File msg = folder.newFile("sample.msg");
		EmailExport.saveAsMsg(message, msg);
		assertTrue(msg.length() > 500);

		OutlookMessage loaded = new OutlookMessage(msg);
		assertTrue(loaded.getSubject().contains("MailBox sample") || loaded.getSubject().length() > 0);
		assertNotNull(loaded.getPlainTextBody());
		assertTrue(loaded.getAttachments().size() >= 2);
		boolean htmlAttached = false;
		for (ch.astorm.jotlmsg.OutlookMessageAttachment attachment : loaded.getAttachments()) {
			if (attachment.getName() != null && attachment.getName().contains("message-body.html")) {
				htmlAttached = true;
				break;
			}
		}
		assertTrue(htmlAttached);
		assertEquals(1, loaded.getRecipients(ch.astorm.jotlmsg.OutlookMessageRecipient.Type.TO).size());
		assertEquals(1, loaded.getRecipients(ch.astorm.jotlmsg.OutlookMessageRecipient.Type.CC).size());
		org.apache.poi.hsmf.MAPIMessage poi = new org.apache.poi.hsmf.MAPIMessage(msg.getAbsolutePath());
		assertTrue(poi.getDisplayFrom().toLowerCase().contains("tester")
				|| poi.getDisplayFrom().toLowerCase().contains("mailbox"));
		assertTrue(poi.getDisplayTo().toLowerCase().contains("developer"));
		assertTrue(poi.getDisplayCC().toLowerCase().contains("qa"));
	}

	@Test
	public void writesOriginalEmlBytes() throws Exception {
		String raw = TestEmailFactory.buildRaw();
		ParsedEmail parsed = EmailParser.parse(raw, "tester@mailbox.local", "developer@example.com", new Date(),
				null, false);
		InboxMessage message = message(parsed, raw);
		File eml = folder.newFile("sample-out.eml");
		EmailExport.saveAsEml(message, eml);
		String saved = new String(Files.readAllBytes(eml.toPath()), StandardCharsets.UTF_8);
		assertTrue(saved.contains("multipart/mixed") || saved.contains("text/html"));
		assertTrue(saved.contains("sample.txt"));
	}

	@Test
	public void writesFullScreenshotWithMetadata() throws Exception {
		String raw = TestEmailFactory.buildRaw();
		ParsedEmail parsed = EmailParser.parse(raw, "tester@mailbox.local", "developer@example.com", new Date(),
				null, false);
		InboxMessage message = message(parsed, raw);
		File png = folder.newFile("sample.png");
		EmailExport.saveScreenshot(message, png);
		assertTrue(png.length() > 1000);
		String base = EmailExport.suggestedFileBase(message, "MailBox_Email");
		assertTrue(base.startsWith("MailBox_Email_"));
		assertTrue(base.matches(".*_\\d{8}_\\d{6}$"));
	}

	@Test
	public void sampleFactoryIncludesAttachmentsAndHtml() {
		String raw = TestEmailFactory.buildRaw();
		assertTrue(raw.contains("text/html"));
		assertTrue(raw.contains("sample.txt"));
		assertTrue(raw.contains("sample.csv"));
		assertTrue(raw.contains("https://"));
		assertTrue(raw.contains("<table"));
	}

	private static InboxMessage message(ParsedEmail parsed, String raw) {
		EmailModel email = new EmailModel();
		email.setFrom("tester@mailbox.local");
		email.setTo("developer@example.com");
		email.setSubject(parsed.getSubject());
		email.setReceivedDate(new Date());
		email.setEmailStr(raw);
		email.setFilePath("memory-export-test-" + System.nanoTime());
		return new InboxMessage(email, parsed, false);
	}
}
